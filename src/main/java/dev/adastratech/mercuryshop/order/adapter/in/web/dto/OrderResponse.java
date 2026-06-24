package dev.adastratech.mercuryshop.order.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Resposta de pedido. Não expõe campos internos (version, idempotencyKey). */
public record OrderResponse(
        UUID id,
        UUID userId,
        String status,
        BigDecimal total,
        List<OrderItemResponse> items,
        Instant createdAt) {
}
