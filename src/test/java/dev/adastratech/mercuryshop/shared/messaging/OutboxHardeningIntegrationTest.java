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
 * Robustez do outbox (Fase 10): um evento impublicável (poison) é parqueado como FAILED sem bloquear
 * a publicação dos eventos válidos seguintes; e o purger remove eventos já publicados.
 */
class OutboxHardeningIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OutboxRepository outbox;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void poisonEventIsParkedAsFailedAndDoesNotBlockValidOnes() throws Exception {
        // Evento poison: tipo (classe) inexistente → nunca desserializa. Gravado ANTES do válido,
        // então é o pendente mais antigo e, sem tratamento, bloquearia a fila para sempre.
        OutboxMessage poison = OutboxMessage.create(
                "dev.adastratech.mercuryshop.NaoExisteEvent", "test.unrouted", "{}");
        outbox.save(poison);

        OrderPaidEvent valid = new OrderPaidEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"), Instant.now());
        OutboxMessage validMessage = OutboxMessage.create(
                OrderPaidEvent.class.getName(), "test.unrouted", objectMapper.writeValueAsString(valid));
        outbox.save(validMessage);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(status(poison.id())).isEqualTo("FAILED");
            assertThat(status(validMessage.id())).isEqualTo("PUBLISHED");
        });
    }

    @Test
    void purgeRemovesPublishedEvents() throws Exception {
        OrderPaidEvent event = new OrderPaidEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("5.00"), Instant.now());
        OutboxMessage message = OutboxMessage.create(
                OrderPaidEvent.class.getName(), "test.unrouted", objectMapper.writeValueAsString(event));
        outbox.save(message);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(
                () -> assertThat(status(message.id())).isEqualTo("PUBLISHED"));

        // Corte no futuro → remove tudo que já foi publicado (inclusive este).
        int removed = outbox.purgePublishedBefore(Instant.now().plusSeconds(60));
        assertThat(removed).isGreaterThanOrEqualTo(1);

        Integer remaining = jdbcTemplate.queryForObject(
                "select count(*) from outbox_event where id = ?", Integer.class, message.id());
        assertThat(remaining).isZero();
    }

    private String status(UUID id) {
        return jdbcTemplate.queryForObject(
                "select status from outbox_event where id = ?", String.class, id);
    }
}