package dev.adastratech.mercuryshop.invoice.domain;

import java.time.Instant;
import java.util.UUID;

/** Fatura gerada quando o pedido é pago. Modelo puro. */
public class Invoice {

    private final UUID id;
    private final UUID orderId;
    private final String number;
    private final Instant issuedAt;

    private Invoice(UUID id, UUID orderId, String number, Instant issuedAt) {
        this.id = id;
        this.orderId = orderId;
        this.number = number;
        this.issuedAt = issuedAt;
    }

    public static Invoice issue(UUID orderId, String number) {
        return new Invoice(UUID.randomUUID(), orderId, number, Instant.now());
    }

    public static Invoice reconstitute(UUID id, UUID orderId, String number, Instant issuedAt) {
        return new Invoice(id, orderId, number, issuedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getNumber() {
        return number;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
