package dev.adastratech.mercuryshop.payment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

/**
 * Converte um valor monetário para a menor unidade da moeda, respeitando as casas decimais reais dela
 * (2 para BRL/USD → centavos; 0 para JPY; 3 para BHD/KWD). Evita o bug de assumir sempre 2 casas, que
 * cobraria 100x a mais em moedas sem centavos. Puro no domínio — sem dependência de infraestrutura.
 */
public final class MinorUnits {

    private MinorUnits() {
    }

    public static long of(BigDecimal amount, String currencyCode) {
        int fractionDigits = Math.max(0,
                Currency.getInstance(currencyCode.toUpperCase(Locale.ROOT)).getDefaultFractionDigits());
        return amount.movePointRight(fractionDigits).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
