package dev.adastratech.mercuryshop.user.domain;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Desafio de MFA: entre o login (e-mail+senha ok) e a confirmação do código TOTP, guarda um token
 * curto → userId. Uso único (consumido na confirmação). Implementado no Redis com TTL.
 */
public interface MfaChallengeStore {

    void save(String tokenHash, UUID userId, Duration ttl);

    /** Lê e remove (uso único) o desafio, retornando o usuário se válido. */
    Optional<UUID> consume(String tokenHash);
}
