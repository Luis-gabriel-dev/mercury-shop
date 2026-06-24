package dev.adastratech.mercuryshop.shared.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
class RedisIdempotencyStore implements IdempotencyStore {

    private static final String PREFIX = "idem:";

    private final StringRedisTemplate redis;

    RedisIdempotencyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<UUID> findOrderId(String key) {
        String value = redis.opsForValue().get(PREFIX + key);
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    @Override
    public void save(String key, UUID orderId, Duration ttl) {
        redis.opsForValue().set(PREFIX + key, orderId.toString(), ttl);
    }
}
