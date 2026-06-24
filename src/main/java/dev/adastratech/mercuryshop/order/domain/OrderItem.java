package dev.adastratech.mercuryshop.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item do pedido com SNAPSHOT de nome e preço no momento do checkout (pedidos históricos
 * permanecem estáveis mesmo que o produto mude depois).
 */
public record OrderItem(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {

    public static OrderItem of(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        return new OrderItem(productId, productName, unitPrice, quantity,
                unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }
}
