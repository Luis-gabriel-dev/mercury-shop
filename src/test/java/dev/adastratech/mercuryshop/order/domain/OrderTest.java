package dev.adastratech.mercuryshop.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static OrderItem item(String price, int qty) {
        return OrderItem.of(UUID.randomUUID(), "Produto", new BigDecimal(price), qty);
    }

    @Test
    void placeComputesTotalAndStartsPending() {
        Order order = Order.place(UUID.randomUUID(), List.of(item("10.00", 2), item("5.00", 1)), "key-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotal()).isEqualByComparingTo("25.00");
        assertThat(order.getItems()).hasSize(2);
    }

    @Test
    void cancelOnlyFromPending() {
        Order order = Order.place(UUID.randomUUID(), List.of(item("10.00", 1)), "key-2");

        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        assertThatThrownBy(order::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiresAtLeastOneItem() {
        assertThatThrownBy(() -> Order.place(UUID.randomUUID(), List.of(), "key-3"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
