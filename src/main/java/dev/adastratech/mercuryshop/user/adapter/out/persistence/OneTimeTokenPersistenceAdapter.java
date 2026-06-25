package dev.adastratech.mercuryshop.user.adapter.out.persistence;

import dev.adastratech.mercuryshop.user.domain.OneTimeToken;
import dev.adastratech.mercuryshop.user.domain.OneTimeTokenRepository;
import dev.adastratech.mercuryshop.user.domain.TokenPurpose;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
class OneTimeTokenPersistenceAdapter implements OneTimeTokenRepository {

    private final OneTimeTokenJpaRepository repository;

    OneTimeTokenPersistenceAdapter(OneTimeTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OneTimeToken save(OneTimeToken token) {
        OneTimeTokenJpaEntity entity = repository.findById(token.getId())
                .orElseGet(OneTimeTokenJpaEntity::new);
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setTokenHash(token.getTokenHash());
        entity.setPurpose(token.getPurpose());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsedAt(token.getUsedAt());
        entity.setPayload(token.getPayload());
        if (token.getCreatedAt() != null) {
            entity.setCreatedAt(token.getCreatedAt());
        }
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<OneTimeToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void invalidateAll(UUID userId, TokenPurpose purpose) {
        repository.deleteByUserIdAndPurpose(userId, purpose);
    }

    private OneTimeToken toDomain(OneTimeTokenJpaEntity entity) {
        return OneTimeToken.reconstitute(
                entity.getId(), entity.getUserId(), entity.getTokenHash(), entity.getPurpose(),
                entity.getExpiresAt(), entity.getUsedAt(), entity.getCreatedAt(), entity.getPayload());
    }
}
