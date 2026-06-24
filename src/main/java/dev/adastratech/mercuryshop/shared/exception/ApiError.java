package dev.adastratech.mercuryshop.shared.exception;

/**
 * Formato de erro padrão e seguro da API (briefing seção 10).
 * Nunca carrega stack trace, nome de tabela/coluna ou detalhes internos.
 */
public record ApiError(ErrorBody error) {

    public record ErrorBody(String code, String message, String requestId) {
    }

    public static ApiError of(String code, String message, String requestId) {
        return new ApiError(new ErrorBody(code, message, requestId));
    }
}
