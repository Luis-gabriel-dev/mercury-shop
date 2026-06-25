package dev.adastratech.mercuryshop.user.domain;

/**
 * Porta para envio de e-mails transacionais. Na Fase 2 a implementação é um stub
 * assíncrono que registra o link no log; o worker real (RabbitMQ + DLQ) entra na Fase 4.
 */
public interface EmailSender {

    void sendEmailVerification(String email, String rawToken);

    void sendPasswordReset(String email, String rawToken);

    /** Verificação enviada ao NOVO e-mail ao solicitar uma troca de endereço. */
    void sendEmailChangeVerification(String newEmail, String rawToken);
}
