package dev.adastratech.mercuryshop.user.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Token de uso único (verificação de e-mail / reset de senha / troca de e-mail). Guarda apenas o
 * HASH do token (nunca o valor em texto puro) — briefing seção 7.1. O {@code payload} carrega um
 * valor associado ao token (ex.: o novo e-mail na troca); nulo nos demais propósitos.
 */
public class OneTimeToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final TokenPurpose purpose;
    private final Instant expiresAt;
    private Instant usedAt;
    private final Instant createdAt;
    private final String payload;

    private OneTimeToken(UUID id, UUID userId, String tokenHash, TokenPurpose purpose,
                         Instant expiresAt, Instant usedAt, Instant createdAt, String payload) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
        this.payload = payload;
    }

    public static OneTimeToken issue(UUID userId, String tokenHash, TokenPurpose purpose, Instant expiresAt) {
        return issue(userId, tokenHash, purpose, expiresAt, null);
    }

    public static OneTimeToken issue(UUID userId, String tokenHash, TokenPurpose purpose,
                                     Instant expiresAt, String payload) {
        return new OneTimeToken(UUID.randomUUID(), userId, tokenHash, purpose, expiresAt, null, null, payload);
    }

    public static OneTimeToken reconstitute(UUID id, UUID userId, String tokenHash, TokenPurpose purpose,
                                            Instant expiresAt, Instant usedAt, Instant createdAt, String payload) {
        return new OneTimeToken(id, userId, tokenHash, purpose, expiresAt, usedAt, createdAt, payload);
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

    public String getPayload() {
        return payload;
    }
}
