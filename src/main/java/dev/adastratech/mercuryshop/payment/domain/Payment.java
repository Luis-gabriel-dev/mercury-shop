package dev.adastratech.mercuryshop.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Registro de pagamento de um pedido (gateway stub na Fase 4). Modelo puro. */
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final PaymentStatus status;
    private final BigDecimal amount;
    private final String transactionRef;
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

    /** Pagamento aprovado pelo gateway (stub). */
    public static Payment approve(UUID orderId, BigDecimal amount, String transactionRef) {
        return new Payment(UUID.randomUUID(), orderId, PaymentStatus.APPROVED, amount, transactionRef, null);
    }

    public static Payment reconstitute(UUID id, UUID orderId, PaymentStatus status, BigDecimal amount,
                                       String transactionRef, Instant createdAt) {
        return new Payment(id, orderId, status, amount, transactionRef, createdAt);
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
