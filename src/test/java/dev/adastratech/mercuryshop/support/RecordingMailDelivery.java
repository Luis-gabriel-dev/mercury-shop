package dev.adastratech.mercuryshop.support;

import dev.adastratech.mercuryshop.shared.messaging.EmailMessage;
import dev.adastratech.mercuryshop.user.adapter.in.messaging.MailDelivery;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entrega de e-mail de teste: captura o que o worker processa (para os testes lerem tokens e
 * confirmações) e mantém a falha em destinatário inválido (para exercitar a DLQ).
 */
public class RecordingMailDelivery implements MailDelivery {

    private final Map<String, String> verificationTokens = new ConcurrentHashMap<>();
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();
    private final List<EmailMessage> delivered = new CopyOnWriteArrayList<>();

    @Override
    public void deliver(EmailMessage message) {
        if (message.to() == null || message.to().isBlank()) {
            throw new IllegalArgumentException("Destinatário de e-mail inválido");
        }
        delivered.add(message);
        if (EmailMessage.TYPE_EMAIL_VERIFICATION.equals(message.type())) {
            verificationTokens.put(message.to(), message.payload());
        } else if (EmailMessage.TYPE_PASSWORD_RESET.equals(message.type())) {
            resetTokens.put(message.to(), message.payload());
        }
    }

    public String verificationToken(String to) {
        return verificationTokens.get(to);
    }

    public String resetToken(String to) {
        return resetTokens.get(to);
    }

    public List<EmailMessage> delivered() {
        return delivered;
    }
}
