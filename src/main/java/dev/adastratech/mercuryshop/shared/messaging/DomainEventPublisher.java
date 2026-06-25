package dev.adastratech.mercuryshop.shared.messaging;

/** Porta de saída para publicação de eventos de domínio (implementada sobre RabbitMQ). */
public interface DomainEventPublisher {

    void publishOrderPaid(OrderPaidEvent event);
}
