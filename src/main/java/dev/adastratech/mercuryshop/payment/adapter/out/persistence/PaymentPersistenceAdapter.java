package dev.adastratech.mercuryshop.payment.adapter.out.persistence;

import dev.adastratech.mercuryshop.payment.domain.Payment;
import dev.adastratech.mercuryshop.payment.domain.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class PaymentPersistenceAdapter implements PaymentRepository {

    private final PaymentJpaRepository repository;

    PaymentPersistenceAdapter(PaymentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = repository.findById(payment.getId()).orElseGet(PaymentJpaEntity::new);
        entity.setId(payment.getId());
        entity.setOrderId(payment.getOrderId());
        entity.setStatus(payment.getStatus());
        entity.setAmount(payment.getAmount());
        entity.setTransactionRef(payment.getTransactionRef());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(this::toDomain);
    }

    private Payment toDomain(PaymentJpaEntity entity) {
        return Payment.reconstitute(entity.getId(), entity.getOrderId(), entity.getStatus(),
                entity.getAmount(), entity.getTransactionRef(), entity.getCreatedAt());
    }
}
