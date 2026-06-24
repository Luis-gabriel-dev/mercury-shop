package dev.adastratech.mercuryshop.shared.idempotency;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Guarda o resultado de operações idempotentes por chave (header Idempotency-Key):
 * mesma chave → mesma resposta. Implementado sobre Redis.
 */
public interface IdempotencyStore {

    Optional<UUID> findOrderId(String key);

    void save(String key, UUID orderId, Duration ttl);
}
