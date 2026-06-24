package dev.adastratech.mercuryshop.cart.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Visão do carrinho com preços atuais resolvidos e totais calculados. */
public record CartView(List<Line> lines, BigDecimal total) {

    public record Line(UUID productId, String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }
}
