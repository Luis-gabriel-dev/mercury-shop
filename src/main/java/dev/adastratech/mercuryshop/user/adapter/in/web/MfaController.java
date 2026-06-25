package dev.adastratech.mercuryshop.user.adapter.in.web;

import dev.adastratech.mercuryshop.user.adapter.in.web.dto.MfaCodeRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.MfaSetupResponse;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.MessageResponse;
import dev.adastratech.mercuryshop.user.application.MfaService;
import dev.adastratech.mercuryshop.user.application.MfaSetup;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Gestão do MFA/TOTP do próprio usuário (autenticado). */
@RestController
@RequestMapping("/v1/users/me/mfa")
class MfaController {

    private final MfaService mfaService;

    MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/setup")
    MfaSetupResponse setup(@AuthenticationPrincipal Jwt jwt) {
        MfaSetup setup = mfaService.setup(userId(jwt));
        return new MfaSetupResponse(setup.secret(), setup.otpauthUri());
    }

    @PostMapping("/enable")
    MessageResponse enable(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MfaCodeRequest request) {
        mfaService.enable(userId(jwt), request.code());
        return new MessageResponse("MFA ativado");
    }

    @PostMapping("/disable")
    MessageResponse disable(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MfaCodeRequest request) {
        mfaService.disable(userId(jwt), request.code());
        return new MessageResponse("MFA desativado");
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
