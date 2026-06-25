package dev.adastratech.mercuryshop.shared.dev;

import dev.adastratech.mercuryshop.user.domain.PasswordHasher;
import dev.adastratech.mercuryshop.user.domain.Role;
import dev.adastratech.mercuryshop.user.domain.User;
import dev.adastratech.mercuryshop.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * APENAS DEV: garante um usuário ADMIN já ativo/verificado para facilitar o teste manual
 * (cria se não existir; promove se existir). Nunca roda em produção (@Profile("dev")).
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "mercury.dev.admin-email")
class DevAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final String adminEmail;
    private final String adminPassword;

    DevAdminSeeder(UserRepository users, PasswordHasher passwordHasher,
                   @Value("${mercury.dev.admin-email}") String adminEmail,
                   @Value("${mercury.dev.admin-password:Adm1n!Passw0rd}") String adminPassword) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        users.findByEmail(adminEmail.toLowerCase()).ifPresentOrElse(existing -> {
            existing.addRole(Role.ADMIN);
            if (!existing.isEmailVerified()) {
                existing.verifyEmail();
            }
            users.save(existing);
            log.warn("DEV: usuário promovido a ADMIN -> {}", adminEmail);
        }, () -> {
            User admin = User.register(adminEmail, passwordHasher.hash(adminPassword), "Dev Admin", null);
            admin.verifyEmail();
            admin.addRole(Role.ADMIN);
            users.save(admin);
            log.warn("DEV: admin criado -> {} (senha: {})", adminEmail, adminPassword);
        });
    }
}
