package dev.adastratech.mercuryshop.user.adapter.out.redis;

import dev.adastratech.mercuryshop.user.domain.MfaChallengeStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Desafio de MFA no Redis (chave = hash do token → userId, com TTL). Consumo é uso único. */
@Component
class RedisMfaChallengeStore implements MfaChallengeStore {

    private static final String PREFIX = "mfa:challenge:";

    private final StringRedisTemplate redis;

    RedisMfaChallengeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(String tokenHash, UUID userId, Duration ttl) {
        redis.opsForValue().set(PREFIX + tokenHash, userId.toString(), ttl);
    }

    @Override
    public Optional<UUID> consume(String tokenHash) {
        String key = PREFIX + tokenHash;
        String value = redis.opsForValue().getAndDelete(key);
        return Optional.ofNullable(value).map(UUID::fromString);
    }
}
