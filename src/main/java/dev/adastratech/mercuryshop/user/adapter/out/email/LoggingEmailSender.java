package dev.adastratech.mercuryshop.user.adapter.out.email;

import dev.adastratech.mercuryshop.user.domain.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Stub de envio de e-mail (Fase 2): registra no log o link/token que iria no e-mail,
 * de forma assíncrona. O envio real (worker RabbitMQ + DLQ) entra na Fase 4.
 * Observação: logar o token aqui é aceitável apenas porque ISTO substitui o e-mail em dev.
 */
@Component
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Async
    @Override
    public void sendEmailVerification(String email, String rawToken) {
        log.info("[EMAIL-STUB] verificação de e-mail -> para={} link=/v1/auth/verify?token={}", email, rawToken);
    }

    @Async
    @Override
    public void sendPasswordReset(String email, String rawToken) {
        log.info("[EMAIL-STUB] reset de senha -> para={} token={}", email, rawToken);
    }
}
