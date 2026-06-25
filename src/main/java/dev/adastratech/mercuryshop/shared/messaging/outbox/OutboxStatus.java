package dev.adastratech.mercuryshop.shared.messaging.outbox;

/** Estado de um evento no outbox: aguardando publicação ou já publicado no broker. */
enum OutboxStatus {
    PENDING,
    PUBLISHED
}