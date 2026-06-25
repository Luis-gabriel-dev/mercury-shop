package dev.adastratech.mercuryshop.user.application;

/**
 * Resultado do login: ou já autenticado (tokens emitidos), ou um desafio de MFA pendente
 * ({@code mfaToken} curto, a ser confirmado em /v1/auth/login/mfa com o código TOTP).
 */
public record LoginResult(AuthTokens tokens, String mfaToken) {

    public static LoginResult authenticated(AuthTokens tokens) {
        return new LoginResult(tokens, null);
    }

    public static LoginResult mfaChallenge(String mfaToken) {
        return new LoginResult(null, mfaToken);
    }

    public boolean mfaRequired() {
        return tokens == null;
    }
}
