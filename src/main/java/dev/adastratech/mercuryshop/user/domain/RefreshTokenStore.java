package dev.adastratech.mercuryshop.user.domain;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta para o armazenamento de refresh tokens (no Redis). Mantém um whitelist de
 * tokens válidos por hash → permite rotação e revogação (logout). TTL = vida do refresh.
 */
public interface RefreshTokenStore {

    void save(String tokenHash, UUID userId, Duration ttl);

    Optional<UUID> findUserId(String tokenHash);

    void revoke(String tokenHash);

    /** Revoga todas as sessões (refresh tokens) do usuário — usado no reset/troca de senha. */
    void revokeAllForUser(UUID userId);

    /**
     * Marca um refresh token como rotacionado (consumido): sai do whitelist ativo e passa a ser
     * rastreado como "já usado" por {@code ttl}, para que uma reapresentação seja detectada como reuso.
     */
    void markRotated(String tokenHash, UUID userId, Duration ttl);

    /** Se o token já foi rotacionado (consumido), retorna o usuário — indício de reuso (token roubado). */
    Optional<UUID> findRotated(String tokenHash);
}
