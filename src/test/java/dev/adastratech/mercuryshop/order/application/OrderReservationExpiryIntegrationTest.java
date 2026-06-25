package dev.adastratech.mercuryshop.order.application;

import dev.adastratech.mercuryshop.cart.domain.Cart;
import dev.adastratech.mercuryshop.cart.domain.CartRepository;
import dev.adastratech.mercuryshop.order.domain.OrderStatus;
import dev.adastratech.mercuryshop.payment.application.PaymentService;
import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reserva de estoque por expiração (Modelo A): o estoque é debitado no checkout e devolvido quando um
 * pedido PENDING não pago expira. Um pedido já pago nunca é expirado.
 */
class OrderReservationExpiryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ProductRepository products;
    @Autowired
    private CartRepository carts;

    private UUID newProduct(int stock) {
        return products.save(Product.create("Reserva", null, new BigDecimal("20.00"), stock, null)).getId();
    }

    private UUID placePendingOrder(UUID productId, UUID user) {
        Cart cart = Cart.empty(user);
        cart.add(productId, 1);
        carts.save(cart);
        return checkoutService.checkout(user, "idem-" + UUID.randomUUID()).getId();
    }

    private int stockOf(UUID productId) {
        return products.findById(productId).orElseThrow().getStockQuantity();
    }

    @Test
    void expiresUnpaidPendingOrderAndRestoresStock() {
        UUID productId = newProduct(5);
        UUID user = UUID.randomUUID();
        UUID orderId = placePendingOrder(productId, user);
        assertThat(stockOf(productId)).isEqualTo(4); // reservado no checkout

        // Um sweep com cutoff no passado NÃO expira um pedido recém-criado.
        orderService.expireUnpaidOrders(Instant.now().minus(Duration.ofHours(1)), 100);
        assertThat(orderService.get(orderId, user, false).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(stockOf(productId)).isEqualTo(4);

        // Expirado: o pedido é cancelado e o estoque devolvido.
        assertThat(orderService.expireIfPending(orderId)).isTrue();
        assertThat(orderService.get(orderId, user, false).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockOf(productId)).isEqualTo(5);
    }

    @Test
    void doesNotExpireAPaidOrder() {
        UUID productId = newProduct(5);
        UUID user = UUID.randomUUID();
        UUID orderId = placePendingOrder(productId, user);
        // Pagamento agora é assíncrono: inicia e confirma via webhook (stub).
        paymentService.initiate(orderId, user);
        paymentService.handleWebhook(
                "{\"type\": \"payment_succeeded\", \"orderId\": \"%s\"}".formatted(orderId), null);

        assertThat(orderService.expireIfPending(orderId)).isFalse();
        assertThat(orderService.get(orderId, user, false).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(stockOf(productId)).isEqualTo(4); // estoque permanece debitado
    }
}
