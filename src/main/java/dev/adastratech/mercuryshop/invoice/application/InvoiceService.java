package dev.adastratech.mercuryshop.invoice.application;

import dev.adastratech.mercuryshop.invoice.domain.Invoice;
import dev.adastratech.mercuryshop.invoice.domain.InvoiceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Geração de fatura, idempotente por pedido (uma fatura por pedido). */
@Service
public class InvoiceService {

    private final InvoiceRepository invoices;

    public InvoiceService(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    @Transactional
    public void generateFor(UUID orderId) {
        if (invoices.existsByOrderId(orderId)) {
            return;
        }
        String number = "INV-" + orderId.toString().substring(0, 8).toUpperCase();
        try {
            invoices.save(Invoice.issue(orderId, number));
        } catch (DataIntegrityViolationException alreadyIssued) {
            // corrida: outra entrega já gerou a fatura — idempotente, ignora.
        }
    }
}
