package dev.adastratech.mercuryshop.payment.application;

import dev.adastratech.mercuryshop.payment.domain.Payment;

/** Resultado de iniciar um pagamento: o registro PENDING e o client secret para o front concluir. */
public record PaymentInitiation(Payment payment, String clientSecret) {
}
