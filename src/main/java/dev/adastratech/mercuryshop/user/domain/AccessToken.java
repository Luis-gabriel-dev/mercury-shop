package dev.adastratech.mercuryshop.user.domain;

/** Access token emitido (JWT) e seu tempo de vida em segundos. */
public record AccessToken(String value, long expiresInSeconds) {
}
