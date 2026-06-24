package dev.adastratech.mercuryshop.shared.exception;

import dev.adastratech.mercuryshop.shared.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Converte exceções em respostas no formato padrão e seguro (seção 10), sem vazar
 * stack trace, mensagem interna, nome de tabela/coluna ou versão de framework (seção 7.3).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String message = fields.isBlank() ? "Dados de entrada inválidos" : fields;
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    /** Violação de invariante do domínio (ex.: preço negativo) → 422. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", ex.getMessage());
    }

    /** Corpo malformado ou tipo de parâmetro inválido (ex.: UUID inválido) → 400, sem detalhes internos. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Requisição malformada");
    }

    /** Violação de integridade no banco (ex.: unique/FK) → 409 genérico, sem expor o banco. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation [requestId={}]", MDC.get(RequestIdFilter.MDC_KEY), ex);
        return build(HttpStatus.CONFLICT, "CONFLICT", "A operação conflita com o estado atual dos dados");
    }

    /** Fallback: nunca expõe a causa real; loga o detalhe internamente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error [requestId={}]", MDC.get(RequestIdFilter.MDC_KEY), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erro interno");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        return ResponseEntity.status(status).body(ApiError.of(code, message, requestId));
    }
}
