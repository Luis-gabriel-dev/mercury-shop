package dev.adastratech.mercuryshop.shared.messaging.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga o agendamento usado pelo {@link OutboxRelay} (poll configurável via mercury.outbox.poll-delay). */
@Configuration
@EnableScheduling
class OutboxConfig {
}