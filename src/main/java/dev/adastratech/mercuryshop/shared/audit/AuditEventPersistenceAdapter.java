package dev.adastratech.mercuryshop.shared.audit;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Persiste a trilha de auditoria no Postgres (append-only): só INSERT, nunca update/delete. */
@Component
class AuditEventPersistenceAdapter implements AuditEventStore {

    private final AuditEventJpaRepository repository;

    AuditEventPersistenceAdapter(AuditEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(String event, String detail, String requestId) {
        AuditEventJpaEntity entity = new AuditEventJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setEvent(event);
        entity.setDetail(detail);
        entity.setRequestId(requestId);
        entity.setCreatedAt(Instant.now());
        repository.save(entity);
    }
}
