package dev.adastratech.mercuryshop.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** Propriedades de segurança (prefixo {@code mercury.security}). Segredos vêm de env. */
@ConfigurationProperties(prefix = "mercury.security")
public record SecurityProperties(
        Jwt jwt,
        Duration refreshTokenTtl,
        Login login,
        RateLimit rateLimit,
        Cors cors) {

    public record Jwt(String issuer, Duration accessTokenTtl, String privateKey, String publicKey) {
    }

    public record Login(int maxAttempts, Duration lockDuration) {
    }

    public record RateLimit(long capacity, Duration refillPeriod) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
