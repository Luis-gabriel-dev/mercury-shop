package dev.adastratech.mercuryshop.shared.exception;

/** Requisição bem-formada mas semanticamente inválida (ex.: carrinho vazio no checkout) → 422. */
public class UnprocessableEntityException extends RuntimeException {

    private final String code;

    public UnprocessableEntityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
