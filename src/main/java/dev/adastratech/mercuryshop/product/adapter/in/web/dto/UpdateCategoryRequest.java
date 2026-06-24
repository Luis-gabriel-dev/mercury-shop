package dev.adastratech.mercuryshop.product.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

/** PATCH parcial: campos nulos não são alterados. */
public record UpdateCategoryRequest(

        @Size(max = 120)
        String name,

        @Size(max = 500)
        String description) {
}
