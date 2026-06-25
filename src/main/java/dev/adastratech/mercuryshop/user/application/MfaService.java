package dev.adastratech.mercuryshop.user.application;

import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import dev.adastratech.mercuryshop.shared.exception.UnprocessableEntityException;
import dev.adastratech.mercuryshop.shared.security.AuditLogger;
import dev.adastratech.mercuryshop.user.domain.Totp;
import dev.adastratech.mercuryshop.user.domain.User;
import dev.adastratech.mercuryshop.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de MFA/TOTP: setup (gera segredo + URI do QR), ativação e desativação. */
@Service
public class MfaService {

    private static final String ISSUER = "Mercury Shop";

    private final UserRepository users;
    private final AuditLogger audit;

    public MfaService(UserRepository users, AuditLogger audit) {
        this.users = users;
        this.audit = audit;
    }

    /** Gera o segredo e devolve a URI otpauth:// para o QR. Só passa a valer após {@link #enable}. */
    @Transactional
    public MfaSetup setup(UUID userId) {
        User user = load(userId);
        String secret = Totp.generateSecret();
        user.startMfaSetup(secret);
        users.save(user);
        return new MfaSetup(secret, Totp.otpauthUri(ISSUER, user.getEmail(), secret));
    }

    @Transactional
    public void enable(UUID userId, String code) {
        User user = load(userId);
        requireValidCode(user, code);
        user.enableMfa();
        users.save(user);
        audit.mfaEnabled(userId);
    }

    @Transactional
    public void disable(UUID userId, String code) {
        User user = load(userId);
        requireValidCode(user, code);
        user.disableMfa();
        users.save(user);
        audit.mfaDisabled(userId);
    }

    private void requireValidCode(User user, String code) {
        if (user.getMfaSecret() == null || !Totp.verify(user.getMfaSecret(), code)) {
            throw new UnprocessableEntityException("INVALID_MFA_CODE", "Código MFA inválido");
        }
    }

    private User load(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
