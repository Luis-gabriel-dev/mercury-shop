package dev.adastratech.mercuryshop.shared.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita o cache (Redis). O TTL vem de {@code spring.cache.redis.time-to-live} (application.yml);
 * os valores são serializados via JDK (por isso o modelo cacheado implementa Serializable).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
