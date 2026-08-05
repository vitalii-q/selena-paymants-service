package com.selena.payments.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox_events")
public class PaymentOutboxEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOutboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private int attempts;

    protected PaymentOutboxEntity() {
    }

    public PaymentOutboxEntity(String aggregateType, UUID aggregateId, String eventType,
                               String payload, PaymentOutboxStatus status) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.attempts = 0;
    }

    @PrePersist
    void setCreationTimestamp() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public PaymentOutboxStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public int getAttempts() { return attempts; }

    public void markSent() {
        this.status = PaymentOutboxStatus.SENT;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentOutboxStatus.FAILED;
        this.attempts++;
    }
}
