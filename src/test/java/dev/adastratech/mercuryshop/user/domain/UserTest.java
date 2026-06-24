package dev.adastratech.mercuryshop.user.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void registerStartsPendingWithCustomerRole() {
        User user = User.register("user@example.com", "hash", "Fulano", null);

        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getRoles()).containsExactly(Role.CUSTOMER);
    }

    @Test
    void verifyEmailActivatesAccount() {
        User user = User.register("user@example.com", "hash", null, null);

        user.verifyEmail();

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void normalizesEmailToLowercase() {
        User user = User.register("USER@Example.COM", "hash", null, null);

        assertThat(user.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> User.register("not-an-email", "hash", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rolesGetterIsDefensiveCopy() {
        User user = User.register("user@example.com", "hash", null, null);

        Set<Role> roles = user.getRoles();
        roles.add(Role.ADMIN);

        // Mexer na cópia não afeta os papéis internos.
        assertThat(user.getRoles()).containsExactly(Role.CUSTOMER);
    }
}
