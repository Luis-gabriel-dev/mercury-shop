package dev.adastratech.mercuryshop.shared.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de mensageria: um exchange de eventos e um dead-letter exchange. Cada fila de
 * trabalho aponta para a DLX; ao esgotar as tentativas (config no application.yml), a mensagem
 * é dead-lettered para a DLQ correspondente.
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

    // --- e-mail ---
    @Bean
    Queue emailQueue() {
        return workQueue(Q_EMAIL, Q_EMAIL_DLQ);
    }

    @Bean
    Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(Q_EMAIL_DLQ).build();
    }

    @Bean
    Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(mercuryExchange()).with(RK_EMAIL);
    }

    @Bean
    Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDeadLetterQueue()).to(mercuryDeadLetterExchange()).with(Q_EMAIL_DLQ);
    }

    // --- OrderPaid → notificação ---
    @Bean
    Queue orderPaidNotificationQueue() {
        return workQueue(Q_ORDER_PAID_NOTIFICATION, Q_ORDER_PAID_NOTIFICATION_DLQ);
    }

    @Bean
    Queue orderPaidNotificationDlq() {
        return QueueBuilder.durable(Q_ORDER_PAID_NOTIFICATION_DLQ).build();
    }

    @Bean
    Binding orderPaidNotificationBinding() {
        return BindingBuilder.bind(orderPaidNotificationQueue()).to(mercuryExchange()).with(RK_ORDER_PAID);
    }

    @Bean
    Binding orderPaidNotificationDlqBinding() {
        return BindingBuilder.bind(orderPaidNotificationDlq()).to(mercuryDeadLetterExchange())
                .with(Q_ORDER_PAID_NOTIFICATION_DLQ);
    }

    // --- OrderPaid → fatura ---
    @Bean
    Queue orderPaidInvoiceQueue() {
        return workQueue(Q_ORDER_PAID_INVOICE, Q_ORDER_PAID_INVOICE_DLQ);
    }

    @Bean
    Queue orderPaidInvoiceDlq() {
        return QueueBuilder.durable(Q_ORDER_PAID_INVOICE_DLQ).build();
    }

    @Bean
    Binding orderPaidInvoiceBinding() {
        return BindingBuilder.bind(orderPaidInvoiceQueue()).to(mercuryExchange()).with(RK_ORDER_PAID);
    }

    @Bean
    Binding orderPaidInvoiceDlqBinding() {
        return BindingBuilder.bind(orderPaidInvoiceDlq()).to(mercuryDeadLetterExchange())
                .with(Q_ORDER_PAID_INVOICE_DLQ);
    }

    @Bean
    Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    private static Queue workQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }
}
