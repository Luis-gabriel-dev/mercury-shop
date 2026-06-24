package dev.adastratech.mercuryshop.order.application;

import dev.adastratech.mercuryshop.cart.domain.Cart;
import dev.adastratech.mercuryshop.cart.domain.CartRepository;
import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.shared.exception.ConflictException;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de concorrência: N unidades em estoque, M > N compradores em paralelo no último item.
 * Invariante crítica (briefing §12): o estoque NUNCA fica negativo e não há venda além do disponível.
 */
class ConcurrentCheckoutTest extends IntegrationTestSupport {

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private ProductRepository products;
    @Autowired
    private CartRepository carts;

    @Test
    void stockNeverGoesNegativeUnderConcurrency() throws Exception {
        int stock = 5;
        int buyers = 12;
        Product product = products.save(
                Product.create("Edição limitada", null, new BigDecimal("50.00"), stock, null));
        UUID productId = product.getId();

        List<UUID> userIds = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            UUID userId = UUID.randomUUID();
            userIds.add(userId);
            Cart cart = Cart.empty(userId);
            cart.add(productId, 1);
            carts.save(cart);
        }

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (UUID userId : userIds) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    checkoutService.checkout(userId, "idem-" + userId);
                    return true;
                } catch (ConflictException outOfStockOrContention) {
                    return false;
                }
            }));
        }
        start.countDown();

        int successes = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successes++;
            }
        }
        pool.shutdown();

        int finalStock = products.findById(productId).orElseThrow().getStockQuantity();
        assertThat(finalStock).isGreaterThanOrEqualTo(0);     // nunca negativo
        assertThat(successes).isBetween(1, stock);            // não vende além do disponível
        assertThat(finalStock).isEqualTo(stock - successes);  // consistência: cada sucesso baixou exatamente 1
    }
}
