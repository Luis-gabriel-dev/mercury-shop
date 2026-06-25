package dev.adastratech.mercuryshop.order.application;

import dev.adastratech.mercuryshop.cart.domain.Cart;
import dev.adastratech.mercuryshop.cart.domain.CartItem;
import dev.adastratech.mercuryshop.cart.domain.CartRepository;
import dev.adastratech.mercuryshop.order.domain.Order;
import dev.adastratech.mercuryshop.order.domain.OrderItem;
import dev.adastratech.mercuryshop.order.domain.OrderRepository;
import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.shared.exception.ConflictException;
import dev.adastratech.mercuryshop.shared.exception.UnprocessableEntityException;
import dev.adastratech.mercuryshop.shared.idempotency.IdempotencyStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Checkout transacional com baixa de estoque sob **lock otimista** (retry com backoff) e
 * **idempotência** via Idempotency-Key. Garante que o estoque nunca fica negativo.
 */
@Service
public class CheckoutService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final CartRepository carts;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final IdempotencyStore idempotency;
    private final TransactionTemplate transactionTemplate;
    private final Counter ordersPlaced;

    public CheckoutService(CartRepository carts, ProductRepository products, OrderRepository orders,
                           IdempotencyStore idempotency, PlatformTransactionManager transactionManager,
                           MeterRegistry meterRegistry) {
        this.carts = carts;
        this.products = products;
        this.orders = orders;
        this.idempotency = idempotency;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.ordersPlaced = Counter.builder("mercury.orders.placed")
                .description("Pedidos criados no checkout").register(meterRegistry);
    }

    public Order checkout(UUID userId, String idempotencyKey) {
        // Caminho rápido: mesma chave já processada → mesmo pedido.
        Optional<UUID> existing = idempotency.findOrderId(idempotencyKey);
        if (existing.isPresent()) {
            return orders.findById(existing.get())
                    .orElseThrow(() -> new IllegalStateException("Pedido idempotente ausente"));
        }

        Order order;
        try {
            order = placeWithRetry(userId, idempotencyKey);
        } catch (DataIntegrityViolationException race) {
            // Corrida de mesma Idempotency-Key: o UNIQUE protege; retorna o pedido já criado.
            return orders.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> race);
        }

        // Pós-commit (fora da transação do banco): limpa o carrinho e registra a idempotência.
        carts.deleteByUserId(userId);
        idempotency.save(idempotencyKey, order.getId(), IDEMPOTENCY_TTL);
        ordersPlaced.increment();
        return order;
    }

    private Order placeWithRetry(UUID userId, String idempotencyKey) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return transactionTemplate.execute(status -> placeOrder(userId, idempotencyKey));
            } catch (ObjectOptimisticLockingFailureException conflict) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ConflictException(
                            "Não foi possível concluir o checkout por concorrência de estoque; tente novamente.");
                }
                backoff(attempt);
            }
        }
    }

    private Order placeOrder(UUID userId, String idempotencyKey) {
        Cart cart = carts.findByUserId(userId).orElseGet(() -> Cart.empty(userId));
        if (cart.isEmpty()) {
            throw new UnprocessableEntityException("EMPTY_CART", "Carrinho vazio");
        }
        List<OrderItem> items = new ArrayList<>();
        for (CartItem cartItem : cart.items()) {
            Product product = products.findById(cartItem.productId())
                    .orElseThrow(() -> new ConflictException("Produto do carrinho não está mais disponível"));
            if (!product.isActive()) {
                throw new ConflictException("Produto indisponível: " + product.getName());
            }
            if (product.getStockQuantity() < cartItem.quantity()) {
                throw new ConflictException("Estoque insuficiente para " + product.getName());
            }
            product.changeStockQuantity(product.getStockQuantity() - cartItem.quantity());
            products.save(product); // @Version detecta concorrência → ObjectOptimisticLockingFailureException
            items.add(OrderItem.of(product.getId(), product.getName(), product.getPrice(), cartItem.quantity()));
        }
        return orders.save(Order.place(userId, items, idempotencyKey));
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(10L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Checkout interrompido", e);
        }
    }
}
