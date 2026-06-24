package dev.adastratech.mercuryshop.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertThat(PasswordPolicy.isStrong("Str0ng!Passw0rd")).isTrue();
    }

    @Test
    void rejectsTooShort() {
        assertThat(PasswordPolicy.isStrong("Ab1!aaaa")).isFalse();
    }

    @Test
    void rejectsMissingComplexity() {
        assertThat(PasswordPolicy.isStrong("abcdefghijklmno")).isFalse();
    }

    @Test
    void validateThrowsOnWeak() {
        assertThatThrownBy(() -> PasswordPolicy.validate("weakpassword"))
                .isInstanceOf(WeakPasswordException.class);
    }
}
