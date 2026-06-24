package dev.adastratech.mercuryshop.user.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.UUID;

/**
 * Modelo de domínio de Usuário — puro, sem dependência de JPA/HTTP/Spring Security.
 * Guarda apenas o hash da senha (nunca a senha em texto puro).
 */
public class User {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UUID id;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private UserStatus status;
    private boolean emailVerified;
    private final EnumSet<Role> roles;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(UUID id, String email, String passwordHash, String fullName, String phone,
                 UserStatus status, boolean emailVerified, Set<Role> roles, Long version,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = normalizeEmail(email);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.fullName = fullName;
        this.phone = phone;
        this.status = status;
        this.emailVerified = emailVerified;
        this.roles = roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Novo cadastro: PENDING_VERIFICATION, e-mail não verificado, papel CUSTOMER. */
    public static User register(String email, String passwordHash, String fullName, String phone) {
        return new User(UUID.randomUUID(), email, passwordHash, fullName, phone,
                UserStatus.PENDING_VERIFICATION, false, EnumSet.of(Role.CUSTOMER), null, null, null);
    }

    public static User reconstitute(UUID id, String email, String passwordHash, String fullName, String phone,
                                    UserStatus status, boolean emailVerified, Set<Role> roles, Long version,
                                    Instant createdAt, Instant updatedAt) {
        return new User(id, email, passwordHash, fullName, phone, status, emailVerified, roles,
                version, createdAt, updatedAt);
    }

    public void verifyEmail() {
        this.emailVerified = true;
        if (this.status == UserStatus.PENDING_VERIFICATION) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = requirePasswordHash(passwordHash);
    }

    public void updateProfile(String fullName, String phone) {
        if (fullName != null) {
            this.fullName = fullName;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    public void block() {
        this.status = UserStatus.BLOCKED;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == UserStatus.BLOCKED;
    }

    private static String normalizeEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        return email.trim().toLowerCase();
    }

    private static String requirePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        return passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    /** Cópia imutável dos papéis. */
    public Set<Role> getRoles() {
        return EnumSet.copyOf(roles);
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
