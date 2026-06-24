package dev.adastratech.mercuryshop.user.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Token de uso único (verificação de e-mail / reset de senha). Guarda apenas o HASH
 * do token (nunca o valor em texto puro) — briefing seção 7.1.
 */
public class OneTimeToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final TokenPurpose purpose;
    private final Instant expiresAt;
    private Instant usedAt;
    private final Instant createdAt;

    private OneTimeToken(UUID id, UUID userId, String tokenHash, TokenPurpose purpose,
                         Instant expiresAt, Instant usedAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static OneTimeToken issue(UUID userId, String tokenHash, TokenPurpose purpose, Instant expiresAt) {
        return new OneTimeToken(UUID.randomUUID(), userId, tokenHash, purpose, expiresAt, null, null);
    }

    public static OneTimeToken reconstitute(UUID id, UUID userId, String tokenHash, TokenPurpose purpose,
                                            Instant expiresAt, Instant usedAt, Instant createdAt) {
        return new OneTimeToken(id, userId, tokenHash, purpose, expiresAt, usedAt, createdAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isValid(Instant now) {
        return !isUsed() && !isExpired(now);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public TokenPurpose getPurpose() {
        return purpose;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
