package dev.adastratech.mercuryshop.product.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductCommand(
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        UUID categoryId) {
}
