package dev.adastratech.mercuryshop.user.application;

import dev.adastratech.mercuryshop.shared.exception.ConflictException;
import dev.adastratech.mercuryshop.shared.exception.ForbiddenException;
import dev.adastratech.mercuryshop.shared.exception.UnauthorizedException;
import dev.adastratech.mercuryshop.shared.security.AuditLogger;
import dev.adastratech.mercuryshop.shared.security.SecurityProperties;
import dev.adastratech.mercuryshop.user.application.command.LoginCommand;
import dev.adastratech.mercuryshop.user.application.command.RegisterCommand;
import dev.adastratech.mercuryshop.user.application.command.ResetPasswordCommand;
import dev.adastratech.mercuryshop.user.domain.AccessToken;
import dev.adastratech.mercuryshop.user.domain.AccessTokenIssuer;
import dev.adastratech.mercuryshop.user.domain.EmailSender;
import dev.adastratech.mercuryshop.user.domain.LoginAttemptStore;
import dev.adastratech.mercuryshop.user.domain.MfaChallengeStore;
import dev.adastratech.mercuryshop.user.domain.OneTimeToken;
import dev.adastratech.mercuryshop.user.domain.OneTimeTokenRepository;
import dev.adastratech.mercuryshop.user.domain.PasswordHasher;
import dev.adastratech.mercuryshop.user.domain.PasswordPolicy;
import dev.adastratech.mercuryshop.user.domain.RefreshTokenStore;
import dev.adastratech.mercuryshop.user.domain.TokenGenerator;
import dev.adastratech.mercuryshop.user.domain.TokenHashing;
import dev.adastratech.mercuryshop.user.domain.TokenPurpose;
import dev.adastratech.mercuryshop.user.domain.Totp;
import dev.adastratech.mercuryshop.user.domain.User;
import dev.adastratech.mercuryshop.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de autenticação (cadastro, verificação, login, refresh, logout, reset). */
@Service
public class AuthService {

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);
    private static final Duration EMAIL_CHANGE_TTL = Duration.ofHours(1);
    private static final Duration MFA_CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final String GENERIC_LOGIN_ERROR = "E-mail ou senha inválidos";

    private final UserRepository users;
    private final OneTimeTokenRepository tokens;
    private final RefreshTokenStore refreshTokens;
    private final MfaChallengeStore mfaChallenges;
    private final LoginAttemptStore loginAttempts;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;
    private final AccessTokenIssuer accessTokenIssuer;
    private final EmailSender emailSender;
    private final AuditLogger audit;
    private final Duration refreshTtl;

    public AuthService(UserRepository users, OneTimeTokenRepository tokens, RefreshTokenStore refreshTokens,
                       MfaChallengeStore mfaChallenges, LoginAttemptStore loginAttempts,
                       PasswordHasher passwordHasher, TokenGenerator tokenGenerator,
                       AccessTokenIssuer accessTokenIssuer, EmailSender emailSender, AuditLogger audit,
                       SecurityProperties properties) {
        this.users = users;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.mfaChallenges = mfaChallenges;
        this.loginAttempts = loginAttempts;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.accessTokenIssuer = accessTokenIssuer;
        this.emailSender = emailSender;
        this.audit = audit;
        this.refreshTtl = properties.refreshTokenTtl();
    }

    @Transactional
    public User register(RegisterCommand command) {
        PasswordPolicy.validate(command.password());
        User user = User.register(command.email(), passwordHasher.hash(command.password()),
                command.fullName(), command.phone());
        if (users.existsByEmail(user.getEmail())) {
            throw new dev.adastratech.mercuryshop.shared.exception.ConflictException("E-mail já cadastrado");
        }
        User saved = users.save(user);
        issueAndSendToken(saved, TokenPurpose.EMAIL_VERIFICATION, VERIFICATION_TTL);
        audit.registered(saved.getId());
        return saved;
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        OneTimeToken token = requireValidToken(rawToken, TokenPurpose.EMAIL_VERIFICATION);
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Token inválido"));
        user.verifyEmail();
        users.save(user);
        token.markUsed(Instant.now());
        tokens.save(token);
        audit.emailVerified(user.getId());
    }

    public LoginResult login(LoginCommand command) {
        String email = normalize(command.email());
        if (loginAttempts.isLocked(email)) {
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }
        Optional<User> maybeUser = users.findByEmail(email);
        if (maybeUser.isEmpty() || !passwordHasher.matches(command.password(), maybeUser.get().getPasswordHash())) {
            loginAttempts.recordFailure(email);
            audit.loginFailed(email);
            if (loginAttempts.isLocked(email)) {
                audit.accountLocked(email);
            }
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }
        User user = maybeUser.get();
        loginAttempts.reset(email);
        if (user.isBlocked()) {
            throw new ForbiddenException("ACCOUNT_BLOCKED", "Conta bloqueada");
        }
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("EMAIL_NOT_VERIFIED", "E-mail ainda não verificado");
        }
        if (user.isMfaEnabled()) {
            // Senha ok, mas o MFA exige um segundo passo: emite um desafio curto (não emite tokens).
            String mfaRaw = tokenGenerator.generate();
            mfaChallenges.save(TokenHashing.sha256(mfaRaw), user.getId(), MFA_CHALLENGE_TTL);
            return LoginResult.mfaChallenge(mfaRaw);
        }
        audit.loginSucceeded(user.getId());
        return LoginResult.authenticated(issueTokens(user));
    }

    /** Segunda etapa do login com MFA: valida o desafio + o código TOTP e emite os tokens. */
    public AuthTokens loginMfa(String mfaToken, String code) {
        if (mfaToken == null || mfaToken.isBlank()) {
            throw new UnauthorizedException("Desafio MFA inválido");
        }
        UUID userId = mfaChallenges.consume(TokenHashing.sha256(mfaToken))
                .orElseThrow(() -> new UnauthorizedException("Desafio MFA inválido ou expirado"));
        User user = users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Desafio MFA inválido"));
        if (!user.isMfaEnabled() || !Totp.verify(user.getMfaSecret(), code)) {
            throw new UnauthorizedException("Código MFA inválido");
        }
        if (user.isBlocked()) {
            throw new ForbiddenException("ACCOUNT_BLOCKED", "Conta bloqueada");
        }
        audit.loginSucceeded(user.getId());
        return issueTokens(user);
    }

    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token ausente");
        }
        String hash = TokenHashing.sha256(rawRefreshToken);
        Optional<UUID> activeUser = refreshTokens.findUserId(hash);
        if (activeUser.isEmpty()) {
            // Não está ativo. Se já foi rotacionado, alguém reapresentou um token consumido → indício de
            // roubo: revoga TODA a família de refresh tokens do usuário (força novo login em todo lugar).
            refreshTokens.findRotated(hash).ifPresent(compromisedUserId -> {
                refreshTokens.revokeAllForUser(compromisedUserId);
                audit.refreshTokenReuseDetected(compromisedUserId);
            });
            throw new UnauthorizedException("Refresh token inválido");
        }
        UUID userId = activeUser.get();
        refreshTokens.markRotated(hash, userId, refreshTtl); // rotação: consome o token (passa a "usado")
        User user = users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));
        if (user.isBlocked()) {
            throw new ForbiddenException("ACCOUNT_BLOCKED", "Conta bloqueada");
        }
        return issueTokens(user);
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = TokenHashing.sha256(rawRefreshToken);
        refreshTokens.findUserId(hash).ifPresent(audit::loggedOut);
        refreshTokens.revoke(hash);
    }

    @Transactional
    public void forgotPassword(String rawEmail) {
        String email = normalize(rawEmail);
        audit.passwordResetRequested(email);
        // Resposta sempre genérica (não revela se o e-mail existe).
        users.findByEmail(email).ifPresent(user ->
                issueAndSendToken(user, TokenPurpose.PASSWORD_RESET, RESET_TTL));
    }

    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        PasswordPolicy.validate(command.newPassword());
        OneTimeToken token = requireValidToken(command.token(), TokenPurpose.PASSWORD_RESET);
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Token inválido"));
        user.changePasswordHash(passwordHasher.hash(command.newPassword()));
        users.save(user);
        token.markUsed(Instant.now());
        tokens.save(token);
        refreshTokens.revokeAllForUser(user.getId()); // encerra sessões existentes
        audit.passwordReset(user.getId());
    }

    /**
     * Solicita a troca de e-mail (autenticado, exige a senha atual). Emite um token EMAIL_CHANGE
     * carregando o novo endereço e envia a confirmação ao NOVO e-mail — a troca só se efetiva ao
     * confirmar o link, garantindo a posse do novo endereço.
     */
    @Transactional
    public void requestEmailChange(UUID userId, String rawNewEmail, String currentPassword) {
        User user = users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Não autenticado"));
        if (!passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Senha atual inválida");
        }
        String newEmail = normalize(rawNewEmail);
        if (users.existsByEmail(newEmail)) {
            throw new ConflictException("E-mail já em uso");
        }
        tokens.invalidateAll(userId, TokenPurpose.EMAIL_CHANGE);
        String raw = tokenGenerator.generate();
        tokens.save(OneTimeToken.issue(userId, TokenHashing.sha256(raw), TokenPurpose.EMAIL_CHANGE,
                Instant.now().plus(EMAIL_CHANGE_TTL), newEmail));
        emailSender.sendEmailChangeVerification(newEmail, raw);
        audit.emailChangeRequested(userId);
    }

    /** Confirma a troca de e-mail pelo token enviado ao novo endereço e encerra as sessões. */
    @Transactional
    public void confirmEmailChange(String rawToken) {
        OneTimeToken token = requireValidToken(rawToken, TokenPurpose.EMAIL_CHANGE);
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Token inválido"));
        String newEmail = token.getPayload();
        if (newEmail == null || users.existsByEmail(newEmail)) {
            throw new ConflictException("Não foi possível concluir a troca de e-mail");
        }
        user.changeEmail(newEmail);
        users.save(user);
        token.markUsed(Instant.now());
        tokens.save(token);
        refreshTokens.revokeAllForUser(user.getId()); // troca de identidade → encerra sessões
        audit.emailChanged(user.getId());
    }

    private OneTimeToken requireValidToken(String rawToken, TokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Token inválido");
        }
        OneTimeToken token = tokens.findByTokenHash(TokenHashing.sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Token inválido"));
        if (token.getPurpose() != purpose || !token.isValid(Instant.now())) {
            throw new UnauthorizedException("Token inválido ou expirado");
        }
        return token;
    }

    private void issueAndSendToken(User user, TokenPurpose purpose, Duration ttl) {
        tokens.invalidateAll(user.getId(), purpose);
        String raw = tokenGenerator.generate();
        tokens.save(OneTimeToken.issue(user.getId(), TokenHashing.sha256(raw), purpose,
                Instant.now().plus(ttl)));
        if (purpose == TokenPurpose.EMAIL_VERIFICATION) {
            emailSender.sendEmailVerification(user.getEmail(), raw);
        } else {
            emailSender.sendPasswordReset(user.getEmail(), raw);
        }
    }

    private AuthTokens issueTokens(User user) {
        AccessToken access = accessTokenIssuer.issue(user);
        String refreshRaw = tokenGenerator.generate();
        refreshTokens.save(TokenHashing.sha256(refreshRaw), user.getId(), refreshTtl);
        return new AuthTokens(access.value(), access.expiresInSeconds(), refreshRaw, refreshTtl.toSeconds());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
