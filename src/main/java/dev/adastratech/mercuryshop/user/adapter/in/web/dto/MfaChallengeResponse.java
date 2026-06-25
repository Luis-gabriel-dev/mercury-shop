package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

/** Resposta do login quando o MFA está ativo: indica o desafio e o token para a segunda etapa. */
public record MfaChallengeResponse(boolean mfaRequired, String mfaToken) {
}
