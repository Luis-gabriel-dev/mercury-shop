package dev.adastratech.mercuryshop.order.domain;

import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída para persistência de pedidos. */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    PageResult<Order> findByUserId(UUID userId, PageQuery page);

    PageResult<Order> findAll(PageQuery page);

    /** IDs de pedidos ainda PENDING criados antes de {@code cutoff} — usado para expirar reservas. */
    List<UUID> findPendingIdsCreatedBefore(Instant cutoff, int limit);
}
