package dev.adastratech.mercuryshop.user.adapter.in.web;

import dev.adastratech.mercuryshop.user.adapter.in.web.dto.DeleteAccountRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.MessageResponse;
import dev.adastratech.mercuryshop.user.application.DataExport;
import dev.adastratech.mercuryshop.user.application.PrivacyService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Privacidade (LGPD): exportação dos próprios dados e exclusão (anonimização) da conta. */
@RestController
@RequestMapping("/v1/users/me")
class PrivacyController {

    private final PrivacyService privacyService;

    PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @GetMapping("/export")
    DataExport export(@AuthenticationPrincipal Jwt jwt) {
        return privacyService.export(userId(jwt));
    }

    @DeleteMapping
    MessageResponse deleteAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteAccountRequest request) {
        privacyService.deleteAccount(userId(jwt), request.currentPassword());
        return new MessageResponse("Conta excluída (dados pessoais anonimizados).");
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
