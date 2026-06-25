package dev.adastratech.mercuryshop.user.adapter.in.web;

import dev.adastratech.mercuryshop.user.adapter.in.web.dto.ForgotPasswordRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.LoginRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.MessageResponse;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.MfaChallengeResponse;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.MfaLoginRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.RegisterRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.ResetPasswordRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.TokenResponse;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.UserResponse;
import dev.adastratech.mercuryshop.user.application.AuthService;
import dev.adastratech.mercuryshop.user.application.AuthTokens;
import dev.adastratech.mercuryshop.user.application.LoginResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/v1/auth")
class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String REFRESH_PATH = "/v1/auth";

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserWebMapper.toResponse(authService.register(UserWebMapper.toCommand(request)));
    }

    @GetMapping("/verify")
    MessageResponse verify(@RequestParam String token) {
        authService.verifyEmail(token);
        return new MessageResponse("E-mail verificado com sucesso");
    }

    @GetMapping("/confirm-email-change")
    MessageResponse confirmEmailChange(@RequestParam String token) {
        authService.confirmEmailChange(token);
        return new MessageResponse("E-mail atualizado com sucesso");
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(UserWebMapper.toCommand(request));
        if (result.mfaRequired()) {
            // MFA ativo: nenhum token ainda; o cliente confirma o código em /login/mfa.
            return ResponseEntity.ok(new MfaChallengeResponse(true, result.mfaToken()));
        }
        return tokenResponse(result.tokens());
    }

    @PostMapping("/login/mfa")
    ResponseEntity<TokenResponse> loginMfa(@Valid @RequestBody MfaLoginRequest request) {
        return tokenResponse(authService.loginMfa(request.mfaToken(), request.code()));
    }

    private ResponseEntity<TokenResponse> tokenResponse(AuthTokens tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens).toString())
                .body(UserWebMapper.toTokenResponse(tokens));
    }

    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        AuthTokens tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens).toString())
                .body(UserWebMapper.toTokenResponse(tokens));
    }

    @PostMapping("/logout")
    ResponseEntity<MessageResponse> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(new MessageResponse("Logout efetuado"));
    }

    @PostMapping("/forgot-password")
    MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return new MessageResponse("Se o e-mail existir, enviaremos instruções de redefinição.");
    }

    @PostMapping("/reset-password")
    MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(UserWebMapper.toCommand(request));
        return new MessageResponse("Senha redefinida com sucesso");
    }

    private ResponseCookie refreshCookie(AuthTokens tokens) {
        return ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_PATH)
                .maxAge(Duration.ofSeconds(tokens.refreshTokenExpiresInSeconds()))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_PATH)
                .maxAge(0)
                .build();
    }
}
