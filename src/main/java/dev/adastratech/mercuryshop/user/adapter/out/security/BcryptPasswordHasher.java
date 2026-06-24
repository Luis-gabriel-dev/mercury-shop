package dev.adastratech.mercuryshop.user.adapter.out.security;

import dev.adastratech.mercuryshop.user.domain.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Implementa a porta {@link PasswordHasher} com BCrypt (custo definido no PasswordEncoder). */
@Component
class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    BcryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
