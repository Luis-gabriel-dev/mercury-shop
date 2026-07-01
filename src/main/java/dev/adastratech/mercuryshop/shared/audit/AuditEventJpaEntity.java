package dev.adastratech.mercuryshop.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
class AuditEventJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String event;

    @Column(length = 200)
    private String detail;

    @Column(name = "request_id", length = 60)
    private String requestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEventJpaEntity() {
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
