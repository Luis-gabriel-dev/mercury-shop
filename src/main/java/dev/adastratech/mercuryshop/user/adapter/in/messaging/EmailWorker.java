package dev.adastratech.mercuryshop.user.adapter.in.messaging;

import dev.adastratech.mercuryshop.shared.messaging.EmailMessage;
import dev.adastratech.mercuryshop.shared.messaging.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Consome a fila de e-mail e delega o envio. Exceções → retry e, ao esgotar, DLQ. */
@Component
@ConditionalOnProperty(name = "mercury.messaging.consumers.enabled", matchIfMissing = true)
class EmailWorker {

    private final MailDelivery mailDelivery;

    EmailWorker(MailDelivery mailDelivery) {
        this.mailDelivery = mailDelivery;
    }

    @RabbitListener(queues = RabbitConfig.Q_EMAIL)
    void onEmail(EmailMessage message) {
        mailDelivery.deliver(message);
    }
}
