package dev.adastratech.mercuryshop.shared.exception;

/** Credenciais inválidas ou token inválido/expirado → 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
