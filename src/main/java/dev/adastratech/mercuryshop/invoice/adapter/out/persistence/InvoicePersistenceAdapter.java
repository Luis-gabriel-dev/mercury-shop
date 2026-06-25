package dev.adastratech.mercuryshop.invoice.adapter.out.persistence;

import dev.adastratech.mercuryshop.invoice.domain.Invoice;
import dev.adastratech.mercuryshop.invoice.domain.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class InvoicePersistenceAdapter implements InvoiceRepository {

    private final InvoiceJpaRepository repository;

    InvoicePersistenceAdapter(InvoiceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity entity = repository.findById(invoice.getId()).orElseGet(InvoiceJpaEntity::new);
        entity.setId(invoice.getId());
        entity.setOrderId(invoice.getOrderId());
        entity.setNumber(invoice.getNumber());
        entity.setIssuedAt(invoice.getIssuedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return repository.existsByOrderId(orderId);
    }

    @Override
    public Optional<Invoice> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(this::toDomain);
    }

    private Invoice toDomain(InvoiceJpaEntity entity) {
        return Invoice.reconstitute(entity.getId(), entity.getOrderId(), entity.getNumber(), entity.getIssuedAt());
    }
}
