package dev.adastratech.mercuryshop.shared.messaging.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Porta de saída do outbox: gravar eventos e, no relay, reivindicar/concluir os pendentes. */
public interface OutboxRepository {

    /** Persiste um evento como PENDING (chamado na mesma transação da escrita de negócio). */
    void save(OutboxMessage message);

    /**
     * Reivindica até {@code batchSize} eventos pendentes, travando as linhas com
     * {@code FOR UPDATE SKIP LOCKED} — deve ser chamado dentro de uma transação. Réplicas
     * concorrentes nunca pegam a mesma linha.
     */
    List<OutboxMessage> claimPending(int batchSize);

    /** Marca o evento como publicado (na mesma transação que o reivindicou). */
    void markPublished(UUID id);

    /**
     * Parqueia um evento impublicável (poison — ex.: tipo/payload que nunca desserializa) como
     * FAILED. Como a claim só busca PENDING, isso evita que um evento defeituoso bloqueie para
     * sempre a publicação dos eventos seguintes.
     */
    void markFailed(UUID id);

    /** Remove eventos já publicados antes do corte; devolve quantos foram removidos. */
    int purgePublishedBefore(Instant cutoff);
}