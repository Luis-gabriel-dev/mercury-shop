package dev.adastratech.mercuryshop.user.adapter.out.redis;

import dev.adastratech.mercuryshop.user.domain.RefreshTokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Whitelist de refresh tokens válidos no Redis (chave = hash do token → userId, com TTL).
 * Mantém também um conjunto por usuário (refresh:user:{id}) para permitir revogar todas as
 * sessões. Suporta rotação (revogar o antigo, salvar o novo), logout e revogação em massa.
 */
@Component
class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String PREFIX = "refresh:";
    private static final String USER_PREFIX = "refresh:user:";

    private final StringRedisTemplate redis;

    RedisRefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(String tokenHash, UUID userId, Duration ttl) {
        redis.opsForValue().set(PREFIX + tokenHash, userId.toString(), ttl);
        String userKey = USER_PREFIX + userId;
        redis.opsForSet().add(userKey, tokenHash);
        redis.expire(userKey, ttl);
    }

    @Override
    public Optional<UUID> findUserId(String tokenHash) {
        String value = redis.opsForValue().get(PREFIX + tokenHash);
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    @Override
    public void revoke(String tokenHash) {
        String userId = redis.opsForValue().get(PREFIX + tokenHash);
        redis.delete(PREFIX + tokenHash);
        if (userId != null) {
            redis.opsForSet().remove(USER_PREFIX + userId, tokenHash);
        }
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        String userKey = USER_PREFIX + userId;
        Set<String> hashes = redis.opsForSet().members(userKey);
        if (hashes != null) {
            for (String hash : hashes) {
                redis.delete(PREFIX + hash);
            }
        }
        redis.delete(userKey);
    }
}
