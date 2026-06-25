package dev.adastratech.mercuryshop.payment.domain;

/** Ciclo do pagamento: criado (intent no gateway), aprovado ou recusado. */
public enum PaymentStatus {
    PENDING,
    APPROVED,
    DECLINED
}
