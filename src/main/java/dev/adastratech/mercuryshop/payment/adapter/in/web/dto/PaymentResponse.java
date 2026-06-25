package dev.adastratech.mercuryshop.payment.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Resposta do início de pagamento. {@code clientSecret} é o segredo do gateway para o front concluir. */
public record PaymentResponse(UUID orderId, UUID paymentId, String status, BigDecimal amount, String clientSecret) {
}
