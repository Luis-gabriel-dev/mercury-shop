package dev.adastratech.mercuryshop.user.domain;

/** Senha não atende à política de segurança. Mapeada para 422. */
public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException() {
        super("A senha não atende à política de segurança (mínimo 12 caracteres, "
                + "com maiúscula, minúscula, dígito e símbolo)");
    }
}
