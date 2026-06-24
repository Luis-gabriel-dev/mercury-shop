package dev.adastratech.mercuryshop.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Pedido — agregado com itens (snapshots), total e chave de idempotência. Modelo puro. */
public class Order {

    private final UUID id;
    private final UUID userId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final BigDecimal total;
    private final String idempotencyKey;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Order(UUID id, UUID userId, OrderStatus status, List<OrderItem> items, BigDecimal total,
                  String idempotencyKey, Long version, Instant createdAt, Instant updatedAt) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Pedido precisa de ao menos um item");
        }
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.items = List.copyOf(items);
        this.total = total;
        this.idempotencyKey = idempotencyKey;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Cria um pedido PENDING a partir dos itens (total = soma dos subtotais). */
    public static Order place(UUID userId, List<OrderItem> items, String idempotencyKey) {
        BigDecimal total = items == null ? BigDecimal.ZERO
                : items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(UUID.randomUUID(), userId, OrderStatus.PENDING, items, total,
                idempotencyKey, null, null, null);
    }

    public static Order reconstitute(UUID id, UUID userId, OrderStatus status, List<OrderItem> items,
                                     BigDecimal total, String idempotencyKey, Long version,
                                     Instant createdAt, Instant updatedAt) {
        return new Order(id, userId, status, items, total, idempotencyKey, version, createdAt, updatedAt);
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    /** Cancela um pedido pendente. */
    public void cancel() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Apenas pedidos pendentes podem ser cancelados");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public boolean belongsTo(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
