package dev.adastratech.mercuryshop.shared.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AuditEventJpaRepository extends JpaRepository<AuditEventJpaEntity, UUID> {
}
