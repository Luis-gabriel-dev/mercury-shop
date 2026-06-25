package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmação para excluir a própria conta (reautenticação com a senha atual). */
public record DeleteAccountRequest(@NotBlank String currentPassword) {
}
