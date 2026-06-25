package dev.adastratech.mercuryshop.order.adapter.in.scheduling;

import dev.adastratech.mercuryshop.order.application.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Reserva de estoque por expiração (Fase 6, Modelo A). O estoque é debitado no checkout, então um
 * pedido PENDING "segura" o estoque até ser pago. Periodicamente, este sweeper cancela os pedidos
 * PENDING não pagos dentro da janela de pagamento, devolvendo o estoque reservado.
 */
@Component
class ExpiredOrderSweeper {

    private static final Logger log = LoggerFactory.getLogger(ExpiredOrderSweeper.class);
    private static final int BATCH = 100;

    private final OrderService orderService;
    private final Duration paymentWindow;

    ExpiredOrderSweeper(OrderService orderService,
                        @Value("${mercury.orders.payment-window:30m}") Duration paymentWindow) {
        this.orderService = orderService;
        this.paymentWindow = paymentWindow;
    }

    @Scheduled(fixedDelayString = "${mercury.orders.sweep-delay:60000}")
    void sweep() {
        int expired = orderService.expireUnpaidOrders(Instant.now().minus(paymentWindow), BATCH);
        if (expired > 0) {
            log.info("Reserva de estoque: {} pedido(s) PENDING expirado(s) e cancelado(s)", expired);
        }
    }
}
