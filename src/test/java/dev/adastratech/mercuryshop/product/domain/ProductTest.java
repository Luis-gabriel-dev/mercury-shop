package dev.adastratech.mercuryshop.product.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void createsActiveProductWithIdentity() {
        Product product = Product.create("Mouse", "Mouse sem fio", new BigDecimal("99.90"), 10, null);

        assertThat(product.getId()).isNotNull();
        assertThat(product.isActive()).isTrue();
        assertThat(product.getStockQuantity()).isEqualTo(10);
        assertThat(product.getPrice()).isEqualByComparingTo("99.90");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Product.create("  ", null, BigDecimal.ONE, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> Product.create("Mouse", null, new BigDecimal("-0.01"), 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPrice() {
        assertThatThrownBy(() -> Product.create("Mouse", null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeStock() {
        assertThatThrownBy(() -> Product.create("Mouse", null, BigDecimal.ONE, -1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePriceRejectsNegativeValue() {
        Product product = Product.create("Mouse", null, BigDecimal.TEN, 5, null);
        assertThatThrownBy(() -> product.changePrice(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(product.getPrice()).isEqualByComparingTo("10");
    }
}
