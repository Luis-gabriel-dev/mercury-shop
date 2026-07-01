package dev.adastratech.mercuryshop.shared.messaging.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Manutenção do outbox (Fase 10): remove periodicamente os eventos já publicados mais antigos que a
 * janela de retenção, para a tabela não crescer indefinidamente. O DELETE é idempotente, então rodar
 * em várias réplicas é seguro (quem chegar depois apenas remove 0 linhas).
 */
@Component
class OutboxPurger {

    private static final Logger log = LoggerFactory.getLogger(OutboxPurger.class);

    private final OutboxRepository outbox;
    private final Duration retention;

    OutboxPurger(OutboxRepository outbox,
                 @Value("${mercury.outbox.retention:7d}") Duration retention) {
        this.outbox = outbox;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${mercury.outbox.purge-delay:3600000}",
            initialDelayString = "${mercury.outbox.purge-delay:3600000}")
    void purge() {
        int removed = outbox.purgePublishedBefore(Instant.now().minus(retention));
        if (removed > 0) {
            log.info("Outbox: {} eventos publicados anteriores a {} removidos", removed, retention);
        }
    }
}