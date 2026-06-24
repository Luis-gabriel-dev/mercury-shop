package dev.adastratech.mercuryshop.shared.exception;

/** Operação proibida pelo estado do recurso (ex.: e-mail não verificado, conta bloqueada) → 403. */
public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
