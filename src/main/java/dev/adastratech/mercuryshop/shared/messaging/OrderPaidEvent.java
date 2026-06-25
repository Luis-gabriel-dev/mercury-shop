package dev.adastratech.mercuryshop.shared.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Evento de domínio publicado quando um pedido é pago. */
public record OrderPaidEvent(UUID orderId, UUID userId, BigDecimal total, Instant occurredAt) {
}
