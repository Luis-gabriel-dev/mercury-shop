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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Pagamento de pedido (gateway stub: sempre aprova). Marca o pedido como PAID e publica o
 * evento OrderPaid — somente APÓS o commit, para não emitir em caso de rollback.
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

        OrderPaidEvent event = new OrderPaidEvent(order.getId(), order.getUserId(), order.getTotal(), Instant.now());
        publishAfterCommit(() -> events.publishOrderPaid(event));
        paymentsApproved.increment();
        return payment;
    }

    private void publishAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
