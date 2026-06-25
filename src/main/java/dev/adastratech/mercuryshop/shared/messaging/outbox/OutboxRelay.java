package dev.adastratech.mercuryshop.shared.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adastratech.mercuryshop.shared.messaging.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Relay do outbox: a cada tick, drena os eventos pendentes publicando no RabbitMQ. Cada evento é
 * processado em sua própria transação, com a linha travada ({@code FOR UPDATE SKIP LOCKED}) durante
 * a publicação — assim várias réplicas podem rodar o relay sem publicar o mesmo evento duas vezes.
 * Se o broker estiver indisponível, a transação reverte (o evento segue PENDING) e é repetido no
 * próximo tick.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outbox;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    OutboxRelay(OutboxRepository outbox, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                PlatformTransactionManager transactionManager) {
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${mercury.outbox.poll-delay:1000}")
    void publishPending() {
        try {
            while (Boolean.TRUE.equals(transactionTemplate.execute(status -> publishNext()))) {
                // drena enquanto houver pendentes
            }
        } catch (RuntimeException e) {
            // Ex.: broker indisponível. Os eventos seguem PENDING e serão repetidos no próximo tick.
            log.warn("Relay do outbox adiado: {}", e.getMessage());
        }
    }

    private boolean publishNext() {
        List<OutboxMessage> claimed = outbox.claimPending(1);
        if (claimed.isEmpty()) {
            return false;
        }
        OutboxMessage message = claimed.get(0);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, message.routingKey(), deserialize(message));
        outbox.markPublished(message.id());
        return true;
    }

    private Object deserialize(OutboxMessage message) {
        try {
            return objectMapper.readValue(message.payload(), Class.forName(message.type()));
        } catch (ReflectiveOperationException | RuntimeException | java.io.IOException e) {
            throw new IllegalStateException(
                    "Falha ao desserializar evento do outbox (tipo " + message.type() + ")", e);
        }
    }
}