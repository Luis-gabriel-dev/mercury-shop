package dev.adastratech.mercuryshop.order.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByIdempotencyKey(String idempotencyKey);

    Page<OrderJpaEntity> findByUserId(UUID userId, Pageable pageable);
}
