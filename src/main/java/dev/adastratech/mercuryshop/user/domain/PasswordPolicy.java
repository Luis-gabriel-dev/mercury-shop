package dev.adastratech.mercuryshop.user.domain;

/**
 * Política de senha forte (briefing seção 7.1): mínimo 12 caracteres, com letra
 * maiúscula, minúscula, dígito e símbolo. Validada no cadastro, reset e troca de senha.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;

    private PasswordPolicy() {
    }

    public static boolean isStrong(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean symbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                symbol = true;
            }
        }
        return upper && lower && digit && symbol;
    }

    /** Lança {@link WeakPasswordException} se a senha não atender à política. */
    public static void validate(String password) {
        if (!isStrong(password)) {
            throw new WeakPasswordException();
        }
    }
}
