package dev.adastratech.mercuryshop.user.adapter.in.messaging;

import dev.adastratech.mercuryshop.shared.messaging.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Envio stub: registra o e-mail no log. Destinatário inválido falha → vai para a DLQ. */
@Component
class LoggingMailDelivery implements MailDelivery {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailDelivery.class);

    @Override
    public void deliver(EmailMessage message) {
        if (message.to() == null || message.to().isBlank()) {
            throw new IllegalArgumentException("Destinatário de e-mail inválido");
        }
        log.info("[EMAIL] enviado type={} para={} payload={}", message.type(), message.to(), message.payload());
    }
}
