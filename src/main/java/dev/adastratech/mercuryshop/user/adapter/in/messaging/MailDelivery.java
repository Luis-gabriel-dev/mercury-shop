package dev.adastratech.mercuryshop.user.adapter.in.messaging;

import dev.adastratech.mercuryshop.shared.messaging.EmailMessage;

/** Entrega final do e-mail (stub na Fase 4). Seam que permite capturar envios nos testes. */
public interface MailDelivery {

    void deliver(EmailMessage message);
}
