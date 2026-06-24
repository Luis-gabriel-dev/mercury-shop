package dev.adastratech.mercuryshop.product.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de produto. Não expõe campos internos (ex.: {@code version}) — briefing seção 7.3.
 */
public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        UUID categoryId,
        boolean active,
        Instant createdAt) {
}
