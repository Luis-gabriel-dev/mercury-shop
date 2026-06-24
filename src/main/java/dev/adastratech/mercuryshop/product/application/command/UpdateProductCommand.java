package dev.adastratech.mercuryshop.product.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/** Campos nulos = sem alteração (PATCH parcial). */
public record UpdateProductCommand(
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        UUID categoryId,
        Boolean active) {
}
