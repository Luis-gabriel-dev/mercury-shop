package dev.adastratech.mercuryshop.shared.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filas de trabalho, DLQs e bindings — declaradas apenas quando os <b>consumidores</b> estão
 * ligados ({@code mercury.messaging.consumers.enabled}, default true). Em produção, a API roda
 * com {@code false} (só publica) e o serviço worker com {@code true} (declara filas e consome).
 */
@Configuration
@ConditionalOnProperty(name = "mercury.messaging.consumers.enabled", matchIfMissing = true)
public class RabbitConsumerConfig {

    private final TopicExchange exchange;
    private final TopicExchange deadLetterExchange;

    RabbitConsumerConfig(@Qualifier("mercuryExchange") TopicExchange exchange,
                         @Qualifier("mercuryDeadLetterExchange") TopicExchange deadLetterExchange) {
        this.exchange = exchange;
        this.deadLetterExchange = deadLetterExchange;
    }

    // --- e-mail ---
    @Bean
    Queue emailQueue() {
        return workQueue(RabbitConfig.Q_EMAIL, RabbitConfig.Q_EMAIL_DLQ);
    }

    @Bean
    Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(RabbitConfig.Q_EMAIL_DLQ).build();
    }

    @Bean
    Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(exchange).with(RabbitConfig.RK_EMAIL);
    }

    @Bean
    Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDeadLetterQueue()).to(deadLetterExchange).with(RabbitConfig.Q_EMAIL_DLQ);
    }

    // --- OrderPaid → notificação ---
    @Bean
    Queue orderPaidNotificationQueue() {
        return workQueue(RabbitConfig.Q_ORDER_PAID_NOTIFICATION, RabbitConfig.Q_ORDER_PAID_NOTIFICATION_DLQ);
    }

    @Bean
    Queue orderPaidNotificationDlq() {
        return QueueBuilder.durable(RabbitConfig.Q_ORDER_PAID_NOTIFICATION_DLQ).build();
    }

    @Bean
    Binding orderPaidNotificationBinding() {
        return BindingBuilder.bind(orderPaidNotificationQueue()).to(exchange).with(RabbitConfig.RK_ORDER_PAID);
    }

    @Bean
    Binding orderPaidNotificationDlqBinding() {
        return BindingBuilder.bind(orderPaidNotificationDlq()).to(deadLetterExchange)
                .with(RabbitConfig.Q_ORDER_PAID_NOTIFICATION_DLQ);
    }

    // --- OrderPaid → fatura ---
    @Bean
    Queue orderPaidInvoiceQueue() {
        return workQueue(RabbitConfig.Q_ORDER_PAID_INVOICE, RabbitConfig.Q_ORDER_PAID_INVOICE_DLQ);
    }

    @Bean
    Queue orderPaidInvoiceDlq() {
        return QueueBuilder.durable(RabbitConfig.Q_ORDER_PAID_INVOICE_DLQ).build();
    }

    @Bean
    Binding orderPaidInvoiceBinding() {
        return BindingBuilder.bind(orderPaidInvoiceQueue()).to(exchange).with(RabbitConfig.RK_ORDER_PAID);
    }

    @Bean
    Binding orderPaidInvoiceDlqBinding() {
        return BindingBuilder.bind(orderPaidInvoiceDlq()).to(deadLetterExchange)
                .with(RabbitConfig.Q_ORDER_PAID_INVOICE_DLQ);
    }

    private static Queue workQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", RabbitConfig.DLX)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }
}
