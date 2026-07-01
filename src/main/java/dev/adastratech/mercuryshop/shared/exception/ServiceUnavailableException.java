package dev.adastratech.mercuryshop.shared.exception;

/**
 * Dependência externa indisponível (ex.: gateway de pagamento fora ou lento após retries / circuito
 * aberto). Mapeada para HTTP 503, sinalizando ao cliente que é seguro tentar de novo mais tarde.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
