package dev.adastratech.mercuryshop.user.application;

import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;
import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import dev.adastratech.mercuryshop.shared.exception.UnauthorizedException;
import dev.adastratech.mercuryshop.shared.security.AuditLogger;
import dev.adastratech.mercuryshop.user.application.command.ChangePasswordCommand;
import dev.adastratech.mercuryshop.user.application.command.UpdateProfileCommand;
import dev.adastratech.mercuryshop.user.domain.PasswordHasher;
import dev.adastratech.mercuryshop.user.domain.PasswordPolicy;
import dev.adastratech.mercuryshop.user.domain.RefreshTokenStore;
import dev.adastratech.mercuryshop.user.domain.User;
import dev.adastratech.mercuryshop.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de conta: dados próprios, troca de senha e listagem administrativa. */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenStore refreshTokens;
    private final AuditLogger audit;

    public UserService(UserRepository users, PasswordHasher passwordHasher,
                       RefreshTokenStore refreshTokens, AuditLogger audit) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public PageResult<User> list(PageQuery page) {
        return users.findAll(page);
    }

    @Transactional
    public User updateProfile(UUID id, UpdateProfileCommand command) {
        User user = get(id);
        user.updateProfile(command.fullName(), command.phone());
        return users.save(user);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordCommand command) {
        User user = get(id);
        if (!passwordHasher.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Senha atual inválida");
        }
        PasswordPolicy.validate(command.newPassword());
        user.changePasswordHash(passwordHasher.hash(command.newPassword()));
        users.save(user);
        refreshTokens.revokeAllForUser(id); // encerra sessões existentes
        audit.passwordChanged(id);
    }
}
