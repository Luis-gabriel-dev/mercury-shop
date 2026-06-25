package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Segunda etapa do login com MFA: o token do desafio + o código TOTP. */
public record MfaLoginRequest(@NotBlank String mfaToken, @NotBlank String code) {
}
