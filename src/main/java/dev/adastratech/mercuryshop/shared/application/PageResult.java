package dev.adastratech.mercuryshop.shared.application;

import java.util.List;
import java.util.function.Function;

/**
 * Resultado paginado independente de framework. Os adapters de persistência convertem
 * o {@code Page} do Spring Data para este tipo; o web converte para DTO de resposta.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(
                content.stream().map(mapper).toList(),
                page, size, totalElements, totalPages);
    }
}
