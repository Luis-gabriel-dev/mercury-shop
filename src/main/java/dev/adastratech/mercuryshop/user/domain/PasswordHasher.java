package dev.adastratech.mercuryshop.user.domain;

/** Porta para hashing de senha (implementada com BCrypt, custo ≥ 12). */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
