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

import java.util.UUID;

/** Consulta e cancelamento de pedidos (com restauração de estoque sob lock otimista). */
@Service
public class OrderService {

    private static final int MAX_ATTEMPTS = 5;

    private final OrderRepository orders;
    private final ProductRepository products;
    private final TransactionTemplate transactionTemplate;
    private final Counter ordersCancelled;

    public OrderService(OrderRepository orders, ProductRepository products,
                        PlatformTransactionManager transactionManager, MeterRegistry meterRegistry) {
        this.orders = orders;
        this.products = products;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.ordersCancelled = Counter.builder("mercury.orders.cancelled")
                .description("Pedidos cancelados").register(meterRegistry);
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
        // Restaura o estoque reservado no checkout.
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
