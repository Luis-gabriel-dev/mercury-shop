package dev.adastratech.mercuryshop.shared.security;

import dev.adastratech.mercuryshop.shared.audit.AuditEventStore;
import dev.adastratech.mercuryshop.shared.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Eventos de segurança (briefing seção 7.8) com request_id e SEM dados sensíveis — e-mails são
 * mascarados, senhas/tokens nunca aparecem. Cada evento é logado (JSON/ECS) e persistido na trilha
 * de auditoria append-only ({@link AuditEventStore}), para ficar consultável/retenível.
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final AuditEventStore store;

    public AuditLogger(AuditEventStore store) {
        this.store = store;
    }

    public void registered(UUID userId) {
        emit("USER_REGISTERED", "userId=" + userId);
    }

    public void emailVerified(UUID userId) {
        emit("EMAIL_VERIFIED", "userId=" + userId);
    }

    public void loginSucceeded(UUID userId) {
        emit("LOGIN_SUCCEEDED", "userId=" + userId);
    }

    public void loginFailed(String email) {
        emit("LOGIN_FAILED", "email=" + mask(email));
    }

    public void accountLocked(String email) {
        emit("ACCOUNT_LOCKED", "email=" + mask(email));
    }

    public void passwordResetRequested(String email) {
        emit("PASSWORD_RESET_REQUESTED", "email=" + mask(email));
    }

    public void passwordReset(UUID userId) {
        emit("PASSWORD_RESET", "userId=" + userId);
    }

    public void passwordChanged(UUID userId) {
        emit("PASSWORD_CHANGED", "userId=" + userId);
    }

    public void loggedOut(UUID userId) {
        emit("LOGOUT", "userId=" + userId);
    }

    public void refreshTokenReuseDetected(UUID userId) {
        emit("REFRESH_TOKEN_REUSE_DETECTED", "userId=" + userId);
    }

    public void emailChangeRequested(UUID userId) {
        emit("EMAIL_CHANGE_REQUESTED", "userId=" + userId);
    }

    public void emailChanged(UUID userId) {
        emit("EMAIL_CHANGED", "userId=" + userId);
    }

    public void mfaEnabled(UUID userId) {
        emit("MFA_ENABLED", "userId=" + userId);
    }

    public void mfaDisabled(UUID userId) {
        emit("MFA_DISABLED", "userId=" + userId);
    }

    public void accountDeleted(UUID userId) {
        emit("ACCOUNT_DELETED", "userId=" + userId);
    }

    private void emit(String event, String detail) {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        log.info("event={} {} requestId={}", event, detail, requestId);
        store.append(event, detail, requestId);
    }

    private static String mask(String email) {
        if (email == null || email.isBlank()) {
            return "?";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
