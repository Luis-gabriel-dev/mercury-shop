package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

/** Resposta de login/refresh. O refresh token NÃO vai aqui — vai em cookie HttpOnly. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse bearer(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, "Bearer", expiresIn);
    }
}
