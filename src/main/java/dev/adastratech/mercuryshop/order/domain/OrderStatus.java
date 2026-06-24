package dev.adastratech.mercuryshop.order.domain;

/** Estados do pedido. Na Fase 3 usamos PENDING e CANCELLED; os demais entram na Fase 4. */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
