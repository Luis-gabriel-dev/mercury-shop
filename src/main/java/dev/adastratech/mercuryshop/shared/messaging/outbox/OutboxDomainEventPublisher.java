package dev.adastratech.mercuryshop.shared.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adastratech.mercuryshop.shared.messaging.DomainEventPublisher;
import dev.adastratech.mercuryshop.shared.messaging.OrderPaidEvent;
import dev.adastratech.mercuryshop.shared.messaging.RabbitConfig;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de domínio gravando-os no <b>outbox</b> (mesma transação da escrita de negócio),
 * em vez de enviar direto ao broker. O {@link OutboxRelay} faz a publicação no RabbitMQ depois,
 * garantindo entrega at-least-once mesmo se o broker estiver fora no instante do commit.
 */
@Component
class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;

    OutboxDomainEventPublisher(OutboxRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishOrderPaid(OrderPaidEvent event) {
        outbox.save(OutboxMessage.create(
                event.getClass().getName(), RabbitConfig.RK_ORDER_PAID, serialize(event)));
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar evento para o outbox", e);
        }
    }
}