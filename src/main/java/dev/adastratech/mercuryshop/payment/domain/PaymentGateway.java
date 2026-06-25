package dev.adastratech.mercuryshop.payment.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Porta de saída para o gateway de pagamento (Stripe em prod; stub em dev/test). */
public interface PaymentGateway {

    /** Cria a cobrança para o pedido e devolve a referência + o segredo para o cliente concluir. */
    PaymentIntent createIntent(UUID orderId, BigDecimal amount, String currency);

    /** Verifica a assinatura e normaliza o evento do webhook. Assinatura inválida → exceção (400). */
    PaymentEvent parseEvent(String payload, String signatureHeader);

    /** Cobrança criada: referência (ex.: PaymentIntent id) e client secret para o front concluir. */
    record PaymentIntent(String reference, String clientSecret) {
    }

    /** Evento do webhook já normalizado (independente do gateway concreto). */
    record PaymentEvent(String eventId, Type type, UUID orderId, String reference) {

        public enum Type {
            SUCCEEDED,
            FAILED,
            IGNORED
        }
    }
}
