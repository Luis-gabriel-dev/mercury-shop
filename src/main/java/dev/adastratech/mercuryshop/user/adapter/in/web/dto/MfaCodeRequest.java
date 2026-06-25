package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Código TOTP de 6 dígitos para ativar/desativar o MFA. */
public record MfaCodeRequest(@NotBlank String code) {
}
