package dev.adastratech.mercuryshop.shared.exception;

/** Requisição inválida (ex.: assinatura de webhook inválida) → 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
