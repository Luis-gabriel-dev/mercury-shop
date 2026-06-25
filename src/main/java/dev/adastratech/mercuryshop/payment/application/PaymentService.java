package dev.adastratech.mercuryshop.payment.application;

import dev.adastratech.mercuryshop.order.domain.Order;
import dev.adastratech.mercuryshop.order.domain.OrderRepository;
import dev.adastratech.mercuryshop.payment.domain.Payment;
import dev.adastratech.mercuryshop.payment.domain.PaymentGateway;
import dev.adastratech.mercuryshop.payment.domain.PaymentRepository;
import dev.adastratech.mercuryshop.shared.exception.ConflictException;
import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import dev.adastratech.mercuryshop.shared.messaging.DomainEventPublisher;
import dev.adastratech.mercuryshop.shared.messaging.OrderPaidEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Pagamento via gateway (Stripe em prod; stub em dev/test). Pagar é assíncrono:
 * {@link #initiate} cria a cobrança e deixa o pedido PENDING; a confirmação chega pelo
 * {@link #handleWebhook}, que verifica a assinatura, marca o pedido PAID e grava o evento OrderPaid
 * no outbox (Fase 6) — na mesma transação. Idempotente: webhook repetido não reprocessa.
 */
@Service
public class PaymentService {

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final DomainEventPublisher events;
    private final String currency;
    private final Counter paymentsApproved;

    public PaymentService(OrderRepository orders, PaymentRepository payments, PaymentGateway gateway,
                          DomainEventPublisher events,
                          @Value("${mercury.payment.currency:brl}") String currency,
                          MeterRegistry meterRegistry) {
        this.orders = orders;
        this.payments = payments;
        this.gateway = gateway;
        this.events = events;
        this.currency = currency;
        this.paymentsApproved = Counter.builder("mercury.payments.approved")
                .description("Pagamentos aprovados").register(meterRegistry);
    }

    /** Inicia o pagamento de um pedido PENDING: cria a cobrança no gateway e devolve o client secret. */
    @Transactional
    public PaymentInitiation initiate(UUID orderId, UUID requesterId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
        if (!order.belongsTo(requesterId)) {
            throw new NotFoundException("Pedido não encontrado"); // não revela pedidos de terceiros
        }
        if (!order.isPending()) {
            throw new ConflictException("Pedido não está pendente de pagamento");
        }
        PaymentGateway.PaymentIntent intent = gateway.createIntent(orderId, order.getTotal(), currency);
        Payment payment = payments.findByOrderId(orderId)
                .map(existing -> {
                    existing.reinitiate(intent.reference());
                    return existing;
                })
                .orElseGet(() -> Payment.initiated(orderId, order.getTotal(), intent.reference()));
        return new PaymentInitiation(payments.save(payment), intent.clientSecret());
    }

    /** Processa o webhook do gateway: verifica a assinatura e, no sucesso, confirma o pagamento. */
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        PaymentGateway.PaymentEvent event = gateway.parseEvent(payload, signatureHeader);
        if (event.type() == PaymentGateway.PaymentEvent.Type.IGNORED || event.orderId() == null) {
            return;
        }
        Order order = orders.findById(event.orderId()).orElse(null);
        if (order == null || !order.isPending()) {
            return; // pedido desconhecido ou já processado → idempotente (responde 200)
        }
        if (event.type() == PaymentGateway.PaymentEvent.Type.SUCCEEDED) {
            Payment payment = payments.findByOrderId(order.getId())
                    .orElseGet(() -> Payment.initiated(order.getId(), order.getTotal(), event.reference()));
            payment.markApproved();
            payments.save(payment);

            order.markPaid();
            orders.save(order);

            // OrderPaid gravado no outbox, na mesma transação; o relay publica depois (Fase 6).
            events.publishOrderPaid(new OrderPaidEvent(
                    order.getId(), order.getUserId(), order.getTotal(), Instant.now()));
            paymentsApproved.increment();
        } else { // FAILED
            payments.findByOrderId(order.getId()).ifPresent(payment -> {
                payment.markDeclined();
                payments.save(payment);
            });
            // O pedido permanece PENDING — o cliente pode tentar pagar novamente.
        }
    }
}
