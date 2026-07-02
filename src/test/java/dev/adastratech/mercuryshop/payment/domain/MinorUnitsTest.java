package dev.adastratech.mercuryshop.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Conversão para a menor unidade respeita as casas decimais reais de cada moeda (Fase 12). */
class MinorUnitsTest {

    @Test
    void twoDecimalCurrenciesUseCents() {
        assertThat(MinorUnits.of(new BigDecimal("10.00"), "BRL")).isEqualTo(1000L);
        assertThat(MinorUnits.of(new BigDecimal("10.50"), "usd")).isEqualTo(1050L); // case-insensitive
    }

    @Test
    void zeroDecimalCurrencyHasNoMinorUnit() {
        // JPY não tem centavos: 1000 ienes == 1000 (e não 100000, que era o bug de assumir 2 casas).
        assertThat(MinorUnits.of(new BigDecimal("1000"), "JPY")).isEqualTo(1000L);
    }

    @Test
    void threeDecimalCurrencyUsesThousandths() {
        assertThat(MinorUnits.of(new BigDecimal("1.234"), "BHD")).isEqualTo(1234L);
    }
}
