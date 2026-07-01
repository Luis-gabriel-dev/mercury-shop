package dev.adastratech.mercuryshop.payment.adapter.out.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adastratech.mercuryshop.payment.domain.PaymentGateway;
import dev.adastratech.mercuryshop.shared.exception.BadRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gateway stub — default (dev/test): cria uma cobrança fake e interpreta um webhook em JSON simples,
 * sem credenciais nem rede. O webhook do stub não exige assinatura. Em produção use o provider
 * {@code stripe} ({@link StripePaymentGateway}). Formato do evento do stub:
 * {@code {"type":"payment_succeeded"|"payment_failed","orderId":"<uuid>"}}.
 */
@Component("paymentGatewayDelegate")
@ConditionalOnProperty(name = "mercury.payment.provider", havingValue = "stub", matchIfMissing = true)
class StubPaymentGateway implements PaymentGateway {

    private final ObjectMapper objectMapper;

    StubPaymentGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentIntent createIntent(UUID orderId, BigDecimal amount, String currency) {
        String reference = "pi_stub_" + UUID.randomUUID();
        return new PaymentIntent(reference, reference + "_secret");
    }

    @Override
    public PaymentEvent parseEvent(String payload, String signatureHeader) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            PaymentEvent.Type type = switch (node.path("type").asText("")) {
                case "payment_succeeded" -> PaymentEvent.Type.SUCCEEDED;
                case "payment_failed" -> PaymentEvent.Type.FAILED;
                default -> PaymentEvent.Type.IGNORED;
            };
            UUID orderId = node.hasNonNull("orderId") ? UUID.fromString(node.get("orderId").asText()) : null;
            String reference = node.path("reference").asText(null);
            String eventId = node.path("id").asText("evt_stub_" + UUID.randomUUID());
            return new PaymentEvent(eventId, type, orderId, reference);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BadRequestException("Payload de webhook inválido");
        }
    }
}
