package dev.adastratech.mercuryshop.product.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(

        @NotBlank
        @Size(max = 160)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        Integer stockQuantity,

        UUID categoryId) {
}
