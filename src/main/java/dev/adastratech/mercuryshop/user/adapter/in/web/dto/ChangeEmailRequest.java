package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Solicitação de troca de e-mail: novo endereço + senha atual (reautenticação). */
public record ChangeEmailRequest(
        @NotBlank @Email String newEmail,
        @NotBlank String currentPassword) {
}
