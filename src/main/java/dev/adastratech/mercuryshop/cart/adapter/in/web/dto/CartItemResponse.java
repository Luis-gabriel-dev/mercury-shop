package dev.adastratech.mercuryshop.cart.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String name,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
