package dev.adastratech.mercuryshop.shared.observability;

import dev.adastratech.mercuryshop.cart.domain.Cart;
import dev.adastratech.mercuryshop.cart.domain.CartRepository;
import dev.adastratech.mercuryshop.order.application.CheckoutService;
import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica que a métrica custom de pedidos é instrumentada (visível no Micrometer/Prometheus). */
class MetricsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private ProductRepository products;
    @Autowired
    private CartRepository carts;

    @Test
    void ordersPlacedCounterIncrementsOnCheckout() {
        Product product = products.save(
                Product.create("Métrica", null, new BigDecimal("5.00"), 10, null));
        UUID user = UUID.randomUUID();
        Cart cart = Cart.empty(user);
        cart.add(product.getId(), 1);
        carts.save(cart);

        double before = ordersPlaced();
        checkoutService.checkout(user, "metric-" + UUID.randomUUID());

        assertThat(ordersPlaced()).isGreaterThan(before);
    }

    private double ordersPlaced() {
        Counter counter = meterRegistry.find("mercury.orders.placed").counter();
        return counter == null ? 0d : counter.count();
    }
}
