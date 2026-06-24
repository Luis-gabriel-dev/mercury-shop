package dev.adastratech.mercuryshop.user.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hash determinístico (SHA-256) usado para guardar e procurar tokens de uso único e
 * refresh tokens pelo hash, sem nunca persistir o valor em texto puro.
 * Tokens têm alta entropia (gerados por CSPRNG), então SHA-256 é adequado aqui
 * (diferente de senhas, que usam BCrypt).
 */
public final class TokenHashing {

    private TokenHashing() {
    }

    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
