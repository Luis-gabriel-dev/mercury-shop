package dev.adastratech.mercuryshop.user.adapter.out.email;

import dev.adastratech.mercuryshop.shared.messaging.EmailMessage;
import dev.adastratech.mercuryshop.shared.messaging.RabbitConfig;
import dev.adastratech.mercuryshop.user.domain.EmailSender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementa a porta {@link EmailSender} publicando uma mensagem na fila de e-mail (RabbitMQ).
 * O envio real (stub) acontece no worker, de forma assíncrona e com DLQ.
 */
@Component
class RabbitEmailPublisher implements EmailSender {

    private final RabbitTemplate rabbitTemplate;

    RabbitEmailPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendEmailVerification(String email, String rawToken) {
        publish(new EmailMessage(email, EmailMessage.TYPE_EMAIL_VERIFICATION, rawToken));
    }

    @Override
    public void sendPasswordReset(String email, String rawToken) {
        publish(new EmailMessage(email, EmailMessage.TYPE_PASSWORD_RESET, rawToken));
    }

    @Override
    public void sendEmailChangeVerification(String newEmail, String rawToken) {
        publish(new EmailMessage(newEmail, EmailMessage.TYPE_EMAIL_CHANGE, rawToken));
    }

    private void publish(EmailMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_EMAIL, message);
    }
}
