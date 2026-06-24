package dev.adastratech.mercuryshop.user.adapter.out.redis;

import dev.adastratech.mercuryshop.shared.security.SecurityProperties;
import dev.adastratech.mercuryshop.user.domain.LoginAttemptStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Controle de tentativas de login no Redis. Conta falhas dentro de uma janela; ao atingir
 * o limite, aplica bloqueio temporário (sem marcar o usuário como BLOCKED permanente).
 */
@Component
class RedisLoginAttemptStore implements LoginAttemptStore {

    private static final String FAIL_PREFIX = "login:fail:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final StringRedisTemplate redis;
    private final int maxAttempts;
    private final Duration lockDuration;

    RedisLoginAttemptStore(StringRedisTemplate redis, SecurityProperties properties) {
        this.redis = redis;
        this.maxAttempts = properties.login().maxAttempts();
        this.lockDuration = properties.login().lockDuration();
    }

    @Override
    public boolean isLocked(String email) {
        return Boolean.TRUE.equals(redis.hasKey(LOCK_PREFIX + key(email)));
    }

    @Override
    public void recordFailure(String email) {
        String failKey = FAIL_PREFIX + key(email);
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, lockDuration);
        }
        if (count != null && count >= maxAttempts) {
            redis.opsForValue().set(LOCK_PREFIX + key(email), "1", lockDuration);
            redis.delete(failKey);
        }
    }

    @Override
    public void reset(String email) {
        redis.delete(FAIL_PREFIX + key(email));
        redis.delete(LOCK_PREFIX + key(email));
    }

    @Override
    public Duration lockTimeRemaining(String email) {
        Long ttl = redis.getExpire(LOCK_PREFIX + key(email), TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? Duration.ofSeconds(ttl) : Duration.ZERO;
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
