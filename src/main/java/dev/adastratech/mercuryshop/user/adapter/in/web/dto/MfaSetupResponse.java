package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

/** Resposta do setup de MFA: o segredo (Base32) e a URI otpauth:// para gerar o QR Code. */
public record MfaSetupResponse(String secret, String otpauthUri) {
}
