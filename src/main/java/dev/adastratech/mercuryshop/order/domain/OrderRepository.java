package dev.adastratech.mercuryshop.order.domain;

import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para persistência de pedidos. */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    PageResult<Order> findByUserId(UUID userId, PageQuery page);

    PageResult<Order> findAll(PageQuery page);
}
