package dev.adastratech.mercuryshop.order.application;

import dev.adastratech.mercuryshop.order.domain.Order;
import dev.adastratech.mercuryshop.order.domain.OrderItem;
import dev.adastratech.mercuryshop.order.domain.OrderRepository;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;
import dev.adastratech.mercuryshop.shared.exception.ConflictException;
import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

/**
 * Consulta, cancelamento e fulfillment de pedidos. O cancelamento (manual ou por expiração da
 * reserva) restaura o estoque sob lock otimista, com retry.
 */
@Service
public class OrderService {

    private static final int MAX_ATTEMPTS = 5;

    private final OrderRepository orders;
    private final ProductRepository products;
    private final TransactionTemplate transactionTemplate;
    private final Counter ordersCancelled;
    private final Counter ordersExpired;

    public OrderService(OrderRepository orders, ProductRepository products,
                        PlatformTransactionManager transactionManager, MeterRegistry meterRegistry) {
        this.orders = orders;
        this.products = products;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.ordersCancelled = Counter.builder("mercury.orders.cancelled")
                .description("Pedidos cancelados").register(meterRegistry);
        this.ordersExpired = Counter.builder("mercury.orders.expired")
                .description("Pedidos PENDING cancelados por expiração da reserva").register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public Order get(UUID orderId, UUID requesterId, boolean admin) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
        // Não revela existência de pedidos de outros usuários.
        if (!admin && !order.belongsTo(requesterId)) {
            throw new NotFoundException("Pedido não encontrado");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public PageResult<Order> listOwn(UUID userId, PageQuery page) {
        return orders.findByUserId(userId, page);
    }

    @Transactional(readOnly = true)
    public PageResult<Order> listAll(PageQuery page) {
        return orders.findAll(page);
    }

    /** Fulfillment: marca o pedido como enviado (PAID → SHIPPED). Restrito a ADMIN/STAFF na borda. */
    @Transactional
    public Order ship(UUID orderId) {
        return transition(orderId, Order::markShipped);
    }

    /** Fulfillment: marca o pedido como entregue (SHIPPED → DELIVERED). Restrito a ADMIN/STAFF na borda. */
    @Transactional
    public Order deliver(UUID orderId) {
        return transition(orderId, Order::markDelivered);
    }

    private Order transition(UUID orderId, java.util.function.Consumer<Order> change) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
        try {
            change.accept(order);
        } catch (IllegalStateException invalid) {
            throw new ConflictException(invalid.getMessage());
        }
        return orders.save(order);
    }

    public Order cancel(UUID orderId, UUID requesterId, boolean admin) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                Order cancelled = transactionTemplate.execute(status -> cancelInTransaction(orderId, requesterId, admin));
                ordersCancelled.increment();
                return cancelled;
            } catch (ObjectOptimisticLockingFailureException conflict) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ConflictException("Não foi possível cancelar por concorrência; tente novamente.");
                }
                backoff(attempt);
            }
        }
    }

    private Order cancelInTransaction(UUID orderId, UUID requesterId, boolean admin) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
        if (!admin && !order.belongsTo(requesterId)) {
            throw new NotFoundException("Pedido não encontrado");
        }
        if (!order.isPending()) {
            throw new ConflictException("Apenas pedidos pendentes podem ser cancelados");
        }
        return restoreStockAndCancel(order);
    }

    /**
     * Reserva de estoque por expiração (Modelo A): cancela todos os pedidos PENDING criados antes do
     * {@code cutoff}, devolvendo o estoque. Retorna quantos foram efetivamente expirados.
     */
    public int expireUnpaidOrders(Instant cutoff, int batchSize) {
        int expired = 0;
        for (UUID id : orders.findPendingIdsCreatedBefore(cutoff, batchSize)) {
            if (expireIfPending(id)) {
                expired++;
            }
        }
        return expired;
    }

    /**
     * Cancela o pedido (restaurando estoque) somente se ainda estiver PENDING. Idempotente: se já foi
     * pago/cancelado nesse meio-tempo, não faz nada e retorna {@code false}.
     */
    public boolean expireIfPending(UUID orderId) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                boolean expired = Boolean.TRUE.equals(
                        transactionTemplate.execute(status -> expireInTransaction(orderId)));
                if (expired) {
                    ordersExpired.increment();
                }
                return expired;
            } catch (ObjectOptimisticLockingFailureException conflict) {
                if (attempt >= MAX_ATTEMPTS) {
                    // Desiste por ora; o pedido segue PENDING e será reavaliado no próximo sweep.
                    return false;
                }
                backoff(attempt);
            }
        }
    }

    private Boolean expireInTransaction(UUID orderId) {
        Order order = orders.findById(orderId).orElse(null);
        if (order == null || !order.isPending()) {
            return false;
        }
        restoreStockAndCancel(order);
        return true;
    }

    /** Devolve ao estoque os itens do pedido e o marca como CANCELLED. */
    private Order restoreStockAndCancel(Order order) {
        for (OrderItem item : order.getItems()) {
            products.findById(item.productId()).ifPresent(product -> {
                product.changeStockQuantity(product.getStockQuantity() + item.quantity());
                products.save(product);
            });
        }
        order.cancel();
        return orders.save(order);
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(10L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cancelamento interrompido", e);
        }
    }
}
