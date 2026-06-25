package dev.adastratech.mercuryshop.invoice.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    boolean existsByOrderId(UUID orderId);

    Optional<InvoiceJpaEntity> findByOrderId(UUID orderId);
}
