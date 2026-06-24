package dev.adastratech.mercuryshop.product.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** PATCH parcial: campos nulos não são alterados. */
public record UpdateProductRequest(

        @Size(max = 160)
        String name,

        @Size(max = 2000)
        String description,

        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @PositiveOrZero
        Integer stockQuantity,

        UUID categoryId,

        Boolean active) {
}
