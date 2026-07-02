package dev.adastratech.mercuryshop.user.domain;

import dev.adastratech.mercuryshop.shared.exception.UnprocessableEntityException;

/**
 * Senha não atende à política de segurança. Estende a exceção compartilhada de entidade não
 * processável (código {@code WEAK_PASSWORD}) → 422, tratada pelo handler genérico — assim o
 * {@code shared} não precisa conhecer este tipo do domínio de usuário.
 */
public class WeakPasswordException extends UnprocessableEntityException {

    public WeakPasswordException() {
        super("WEAK_PASSWORD", "A senha não atende à política de segurança (mínimo 12 caracteres, "
                + "com maiúscula, minúscula, dígito e símbolo)");
    }
}
