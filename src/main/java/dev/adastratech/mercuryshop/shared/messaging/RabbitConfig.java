package dev.adastratech.mercuryshop.shared.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Núcleo de mensageria, sempre ativo: o exchange de eventos, o dead-letter exchange e o
 * conversor JSON. O lado <b>publicador</b> (web) depende disto. As filas/consumidores ficam em
 * {@link RabbitConsumerConfig} (condicionais), declaradas pelo serviço worker.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "mercury.exchange";
    public static final String DLX = "mercury.dlx";

    public static final String RK_EMAIL = "email";
    public static final String RK_ORDER_PAID = "order.paid";

    public static final String Q_EMAIL = "q.email";
    public static final String Q_EMAIL_DLQ = "q.email.dlq";
    public static final String Q_ORDER_PAID_NOTIFICATION = "q.order-paid.notification";
    public static final String Q_ORDER_PAID_NOTIFICATION_DLQ = "q.order-paid.notification.dlq";
    public static final String Q_ORDER_PAID_INVOICE = "q.order-paid.invoice";
    public static final String Q_ORDER_PAID_INVOICE_DLQ = "q.order-paid.invoice.dlq";

    @Bean
    TopicExchange mercuryExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    TopicExchange mercuryDeadLetterExchange() {
        return new TopicExchange(DLX);
    }

    @Bean
    Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
