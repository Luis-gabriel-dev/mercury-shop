package dev.adastratech.mercuryshop.product.domain;

import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para persistência de produtos. Implementada por um adapter na camada de infra. */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    PageResult<Product> findAll(ProductFilter filter, PageQuery page);

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
