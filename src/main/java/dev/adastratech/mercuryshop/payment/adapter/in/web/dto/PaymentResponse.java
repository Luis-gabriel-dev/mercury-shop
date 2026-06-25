package dev.adastratech.mercuryshop.payment.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(UUID orderId, UUID paymentId, String status, BigDecimal amount) {
}
