package dev.adastratech.mercuryshop.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro de pagamento de um pedido. Criado como PENDING quando o gateway gera o intent/cobrança e
 * confirmado (APPROVED/DECLINED) ao receber o webhook do gateway. Modelo puro.
 */
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private PaymentStatus status;
    private final BigDecimal amount;
    private String transactionRef;
    private final Instant createdAt;

    private Payment(UUID id, UUID orderId, PaymentStatus status, BigDecimal amount,
                    String transactionRef, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.transactionRef = transactionRef;
        this.createdAt = createdAt;
    }

    /** Pagamento iniciado no gateway (aguardando confirmação via webhook). */
    public static Payment initiated(UUID orderId, BigDecimal amount, String transactionRef) {
        return new Payment(UUID.randomUUID(), orderId, PaymentStatus.PENDING, amount, transactionRef, null);
    }

    public static Payment reconstitute(UUID id, UUID orderId, PaymentStatus status, BigDecimal amount,
                                       String transactionRef, Instant createdAt) {
        return new Payment(id, orderId, status, amount, transactionRef, createdAt);
    }

    /** Reinicia a cobrança (novo intent) de um pagamento ainda não concluído. */
    public void reinitiate(String transactionRef) {
        this.status = PaymentStatus.PENDING;
        this.transactionRef = transactionRef;
    }

    public void markApproved() {
        this.status = PaymentStatus.APPROVED;
    }

    public void markDeclined() {
        this.status = PaymentStatus.DECLINED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
