package dev.adastratech.mercuryshop.shared.messaging.outbox;

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
}