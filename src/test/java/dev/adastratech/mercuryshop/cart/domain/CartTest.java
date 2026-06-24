package dev.adastratech.mercuryshop.cart.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    @Test
    void addAccumulatesQuantity() {
        Cart cart = Cart.empty(UUID.randomUUID());
        UUID product = UUID.randomUUID();

        cart.add(product, 2);
        cart.add(product, 3);

        assertThat(cart.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.productId()).isEqualTo(product);
                    assertThat(item.quantity()).isEqualTo(5);
                });
    }

    @Test
    void setQuantityZeroRemovesItem() {
        Cart cart = Cart.empty(UUID.randomUUID());
        UUID product = UUID.randomUUID();
        cart.add(product, 2);

        cart.setQuantity(product, 0);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void rejectsNonPositiveAdd() {
        Cart cart = Cart.empty(UUID.randomUUID());
        assertThatThrownBy(() -> cart.add(UUID.randomUUID(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeAndClear() {
        Cart cart = Cart.empty(UUID.randomUUID());
        UUID product = UUID.randomUUID();
        cart.add(product, 1);
        cart.add(UUID.randomUUID(), 1);

        cart.remove(product);
        assertThat(cart.items()).hasSize(1);

        cart.clear();
        assertThat(cart.isEmpty()).isTrue();
    }
}
