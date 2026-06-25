package dev.adastratech.mercuryshop.user.application;

/** Dados para o usuário ativar o MFA: o segredo (Base32) e a URI otpauth:// para o QR Code. */
public record MfaSetup(String secret, String otpauthUri) {
}
