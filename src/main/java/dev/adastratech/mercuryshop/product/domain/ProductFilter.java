package dev.adastratech.mercuryshop.product.domain;

import java.util.UUID;

/** Critérios opcionais para listagem de produtos. */
public record ProductFilter(String name, UUID categoryId) {

    public static ProductFilter empty() {
        return new ProductFilter(null, null);
    }

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasCategory() {
        return categoryId != null;
    }
}
