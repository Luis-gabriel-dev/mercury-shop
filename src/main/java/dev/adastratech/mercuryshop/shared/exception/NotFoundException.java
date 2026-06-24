package dev.adastratech.mercuryshop.shared.exception;

/** Recurso não encontrado → 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
