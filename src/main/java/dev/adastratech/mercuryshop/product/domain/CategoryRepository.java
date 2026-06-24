package dev.adastratech.mercuryshop.product.domain;

import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para persistência de categorias. Implementada por um adapter na camada de infra. */
public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(UUID id);

    PageResult<Category> findAll(PageQuery page);

    boolean existsById(UUID id);

    boolean existsByName(String name);

    void deleteById(UUID id);
}
