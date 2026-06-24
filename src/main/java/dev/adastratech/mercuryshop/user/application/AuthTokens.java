package dev.adastratech.mercuryshop.user.application;

/** Resultado de login/refresh: access token (JWT) + refresh token opaco (vai em cookie). */
public record AuthTokens(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds) {
}
