package dev.adastratech.mercuryshop.invoice.domain;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    boolean existsByOrderId(UUID orderId);

    Optional<Invoice> findByOrderId(UUID orderId);
}
