package dev.adastratech.mercuryshop.order.adapter.out.persistence;

import dev.adastratech.mercuryshop.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByIdempotencyKey(String idempotencyKey);

    Page<OrderJpaEntity> findByUserId(UUID userId, Pageable pageable);

    @Query("select e.id from OrderJpaEntity e where e.status = :status and e.createdAt < :cutoff order by e.createdAt")
    List<UUID> findIdsByStatusAndCreatedAtBefore(
            @Param("status") OrderStatus status, @Param("cutoff") Instant cutoff, Pageable pageable);
}
