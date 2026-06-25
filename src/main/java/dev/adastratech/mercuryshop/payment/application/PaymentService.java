package dev.adastratech.mercuryshop.payment.application;

import dev.adastratech.mercuryshop.order.domain.Order;
import dev.adastratech.mercuryshop.order.domain.OrderRepository;
import dev.adastratech.mercuryshop.payment.domain.Payment;
import dev.adastratech.mercuryshop.payment.domain.PaymentRepository;
import dev.adastratech.mercuryshop.shared.exception.ConflictException;
import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import dev.adastratech.mercuryshop.shared.messaging.DomainEventPublisher;
import dev.adastratech.mercuryshop.shared.messaging.OrderPaidEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Pagamento de pedido (gateway stub: sempre aprova). Marca o pedido como PAID e grava o evento
 * OrderPaid no <b>outbox</b>, na MESMA transação — o relay publica no broker depois (at-least-once).
 * Se a transação reverter, o evento não é gravado e portanto nunca é publicado.
 */
@Service
public class PaymentService {

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final DomainEventPublisher events;
    private final Counter paymentsApproved;

    public PaymentService(OrderRepository orders, PaymentRepository payments, DomainEventPublisher events,
                          MeterRegistry meterRegistry) {
        this.orders = orders;
        this.payments = payments;
        this.events = events;
        this.paymentsApproved = Counter.builder("mercury.payments.approved")
                .description("Pagamentos aprovados").register(meterRegistry);
    }

    @Transactional
    public Payment pay(UUID orderId, UUID requesterId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
        // Não revela existência de pedidos de outros usuários.
        if (!order.belongsTo(requesterId)) {
            throw new NotFoundException("Pedido não encontrado");
        }
        if (!order.isPending()) {
            throw new ConflictException("Pedido não está pendente de pagamento");
        }

        Payment payment = payments.save(
                Payment.approve(orderId, order.getTotal(), "stub-" + UUID.randomUUID()));
        order.markPaid();
        orders.save(order);

        // Grava o evento no outbox dentro desta transação; o relay publica no broker depois.
        OrderPaidEvent event = new OrderPaidEvent(order.getId(), order.getUserId(), order.getTotal(), Instant.now());
        events.publishOrderPaid(event);
        paymentsApproved.increment();
        return payment;
    }
}
