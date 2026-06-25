package dev.adastratech.mercuryshop.shared.messaging.outbox;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
class OutboxPersistenceAdapter implements OutboxRepository {

    private final OutboxJpaRepository repository;

    OutboxPersistenceAdapter(OutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(OutboxMessage message) {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(message.id());
        entity.setType(message.type());
        entity.setRoutingKey(message.routingKey());
        entity.setPayload(message.payload());
        entity.setStatus(OutboxStatus.PENDING);
        entity.setAttempts(0);
        entity.setCreatedAt(Instant.now());
        repository.save(entity);
    }

    @Override
    public List<OutboxMessage> claimPending(int batchSize) {
        return repository.findPending(OutboxStatus.PENDING, PageRequest.of(0, batchSize)).stream()
                .map(e -> new OutboxMessage(e.getId(), e.getType(), e.getRoutingKey(), e.getPayload()))
                .toList();
    }

    @Override
    public void markPublished(UUID id) {
        repository.findById(id).ifPresent(entity -> {
            entity.setStatus(OutboxStatus.PUBLISHED);
            entity.setPublishedAt(Instant.now());
            repository.save(entity);
        });
    }
}