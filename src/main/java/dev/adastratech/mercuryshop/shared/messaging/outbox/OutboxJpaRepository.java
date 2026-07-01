package dev.adastratech.mercuryshop.shared.messaging.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * Pendentes mais antigos primeiro, com bloqueio pessimista de escrita e {@code SKIP LOCKED}
     * (hint de timeout -2 no Hibernate) — gera {@code FOR UPDATE SKIP LOCKED} no Postgres, de modo
     * que réplicas concorrentes do relay nunca disputam a mesma linha.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select e from OutboxEventJpaEntity e where e.status = :status order by e.createdAt")
    List<OutboxEventJpaEntity> findPending(@Param("status") OutboxStatus status, Pageable pageable);

    /** Remove eventos num dado status publicados antes do corte; devolve a contagem removida. */
    @Modifying
    @Query("delete from OutboxEventJpaEntity e where e.status = :status and e.publishedAt < :cutoff")
    int deleteByStatusAndPublishedAtBefore(@Param("status") OutboxStatus status,
                                           @Param("cutoff") Instant cutoff);
}