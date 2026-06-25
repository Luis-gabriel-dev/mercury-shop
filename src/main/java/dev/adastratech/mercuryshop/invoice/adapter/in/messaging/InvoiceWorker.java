package dev.adastratech.mercuryshop.invoice.adapter.in.messaging;

import dev.adastratech.mercuryshop.invoice.application.InvoiceService;
import dev.adastratech.mercuryshop.shared.messaging.OrderPaidEvent;
import dev.adastratech.mercuryshop.shared.messaging.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Worker que gera a fatura ao receber OrderPaid. Falhas → retry e, ao esgotar, DLQ. */
@Component
@ConditionalOnProperty(name = "mercury.messaging.consumers.enabled", matchIfMissing = true)
class InvoiceWorker {

    private final InvoiceService invoiceService;

    InvoiceWorker(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @RabbitListener(queues = RabbitConfig.Q_ORDER_PAID_INVOICE)
    void onOrderPaid(OrderPaidEvent event) {
        invoiceService.generateFor(event.orderId());
    }
}
