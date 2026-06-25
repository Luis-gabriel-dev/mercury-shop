package dev.adastratech.mercuryshop.shared.messaging;

import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import dev.adastratech.mercuryshop.support.RecordingMailDelivery;
import dev.adastratech.mercuryshop.user.domain.EmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** E-mail por fila (publisher → worker → entrega) e roteamento de falhas para a DLQ. */
class EmailMessagingIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private EmailSender emailSender; // RabbitEmailPublisher (publica na fila)
    @Autowired
    private RecordingMailDelivery mail;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void emailIsDeliveredThroughQueue() {
        String to = "queue-" + UUID.randomUUID() + "@example.com";

        emailSender.sendEmailVerification(to, "tok-123");

        await().atMost(Duration.ofSeconds(10)).until(() -> mail.verificationToken(to) != null);
        assertThat(mail.verificationToken(to)).isEqualTo("tok-123");
    }

    @Test
    void invalidEmailIsRoutedToDeadLetterQueue() {
        // Destinatário em branco faz o worker falhar; após os retries, vai para a DLQ.
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_EMAIL,
                new EmailMessage("", EmailMessage.TYPE_EMAIL_VERIFICATION, "x"));

        await().atMost(Duration.ofSeconds(15))
                .until(() -> rabbitTemplate.receive(RabbitConfig.Q_EMAIL_DLQ) != null);
    }
}
