package dev.adastratech.mercuryshop.payment.adapter.out.gateway;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import dev.adastratech.mercuryshop.payment.domain.PaymentGateway;
import dev.adastratech.mercuryshop.shared.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gateway real sobre o SDK do Stripe (ativo quando {@code mercury.payment.provider=stripe}). Cria um
 * PaymentIntent com o {@code orderId} em metadata e, no webhook, verifica a assinatura via
 * {@link Webhook#constructEvent} antes de normalizar o evento. Segredos só por variável de ambiente.
 */
@Component
@ConditionalOnProperty(name = "mercury.payment.provider", havingValue = "stripe")
class StripePaymentGateway implements PaymentGateway {

    private final String webhookSecret;

    StripePaymentGateway(@Value("${mercury.payment.stripe.secret-key}") String secretKey,
                         @Value("${mercury.payment.stripe.webhook-secret}") String webhookSecret) {
        Stripe.apiKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public PaymentIntent createIntent(UUID orderId, BigDecimal amount, String currency) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount.movePointRight(2).longValueExact()) // menor unidade da moeda (centavos)
                .setCurrency(currency)
                .putMetadata("orderId", orderId.toString())
                .build();
        try {
            com.stripe.model.PaymentIntent intent = com.stripe.model.PaymentIntent.create(params);
            return new PaymentIntent(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new IllegalStateException("Falha ao criar cobrança no gateway de pagamento", e);
        }
    }

    @Override
    public PaymentEvent parseEvent(String payload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new BadRequestException("Assinatura do webhook ausente");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new BadRequestException("Assinatura do webhook inválida");
        }
        PaymentEvent.Type type = switch (event.getType()) {
            case "payment_intent.succeeded" -> PaymentEvent.Type.SUCCEEDED;
            case "payment_intent.payment_failed" -> PaymentEvent.Type.FAILED;
            default -> PaymentEvent.Type.IGNORED;
        };
        if (type == PaymentEvent.Type.IGNORED) {
            return new PaymentEvent(event.getId(), type, null, null);
        }
        Optional<StripeObject> data = event.getDataObjectDeserializer().getObject();
        if (data.isEmpty() || !(data.get() instanceof com.stripe.model.PaymentIntent intent)) {
            return new PaymentEvent(event.getId(), PaymentEvent.Type.IGNORED, null, null);
        }
        Map<String, String> metadata = intent.getMetadata();
        String orderId = metadata == null ? null : metadata.get("orderId");
        return new PaymentEvent(event.getId(), type,
                orderId == null ? null : UUID.fromString(orderId), intent.getId());
    }
}
