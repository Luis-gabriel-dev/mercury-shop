package dev.adastratech.mercuryshop.shared.messaging.outbox;

import java.util.UUID;

/**
 * Evento pronto para publicação, no formato em que o outbox o armazena: identidade, nome da classe
 * (para o relay desserializar), routing key e o payload JSON. Status/attempts/timestamps são
 * detalhes de persistência preenchidos pelo adapter.
 */
public record OutboxMessage(UUID id, String type, String routingKey, String payload) {

    /** Cria um evento novo, com identidade própria, para ser persistido como PENDING. */
    public static OutboxMessage create(String type, String routingKey, String payload) {
        return new OutboxMessage(UUID.randomUUID(), type, routingKey, payload);
    }
}