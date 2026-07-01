package dev.adastratech.mercuryshop.shared.messaging.outbox;

/**
 * Estado de um evento no outbox: aguardando publicação, já publicado no broker, ou parqueado como
 * FAILED (impublicável/poison) — este último é ignorado pela claim para não bloquear a fila.
 */
enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}