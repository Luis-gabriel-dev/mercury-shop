package dev.adastratech.mercuryshop.shared.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adastratech.mercuryshop.shared.messaging.outbox.OutboxMessage;
import dev.adastratech.mercuryshop.shared.messaging.outbox.OutboxRepository;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Outbox: um evento gravado é publicado pelo relay e marcado como PUBLISHED. Usa uma routing key
 * sem binding para exercitar só o mecanismo do outbox, sem envolver consumidores.
 */
class OutboxRelayIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OutboxRepository outbox;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void relayPublishesPendingEventAndMarksItPublished() throws Exception {
        OrderPaidEvent event = new OrderPaidEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"), Instant.now());
        OutboxMessage message = OutboxMessage.create(
                OrderPaidEvent.class.getName(), "test.unrouted", objectMapper.writeValueAsString(event));

        outbox.save(message);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            String status = jdbcTemplate.queryForObject(
                    "select status from outbox_event where id = ?", String.class, message.id());
            assertThat(status).isEqualTo("PUBLISHED");
        });
        Instant publishedAt = jdbcTemplate.queryForObject(
                "select published_at from outbox_event where id = ?", Instant.class, message.id());
        assertThat(publishedAt).isNotNull();
    }
}
