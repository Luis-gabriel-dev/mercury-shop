package dev.adastratech.mercuryshop.user.domain;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para persistência de tokens de uso único (verificação/reset). */
public interface OneTimeTokenRepository {

    OneTimeToken save(OneTimeToken token);

    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    /** Invalida tokens anteriores do mesmo propósito (um token ativo por vez). */
    void invalidateAll(UUID userId, TokenPurpose purpose);
}
