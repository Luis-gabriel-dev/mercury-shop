package dev.adastratech.mercuryshop.shared.exception;

/** Conflito com o estado atual (ex.: nome único já em uso) → 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
