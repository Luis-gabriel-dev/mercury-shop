package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

import dev.adastratech.mercuryshop.shared.application.PageResult;

import java.util.List;

/** Envelope de paginação para respostas REST de usuários. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(
                result.content(), result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }
}
