package com.selena.payments.entities;

import com.selena.payments.exceptions.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PaymentEntity() {
    }

    public static PaymentEntity create(Long bookingId, UUID userId, BigDecimal amount,
                                       String currency, UUID idempotencyKey) {
        PaymentEntity payment = new PaymentEntity();
        payment.id = UUID.randomUUID();
        payment.bookingId = bookingId;
        payment.userId = userId;
        payment.amount = amount;
        payment.currency = currency;
        payment.idempotencyKey = idempotencyKey;
        payment.status = PaymentStatus.PENDING;
        return payment;
    }

    public void markSucceeded(String transactionId) {
        transitionTo(PaymentStatus.SUCCEEDED);
        providerTransactionId = transactionId;
        failureCode = null;
        failureReason = null;
    }

    public void markFailed(String code, String reason) {
        transitionTo(PaymentStatus.FAILED);
        failureCode = code;
        failureReason = reason;
    }

    public void cancel() {
        transitionTo(PaymentStatus.CANCELLED);
    }

    public void refund() {
        transitionTo(PaymentStatus.REFUNDED);
    }

    private void transitionTo(PaymentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(
                    "INVALID_PAYMENT_STATUS_TRANSITION",
                    "Cannot transition payment from " + status + " to " + target
            );
        }
        status = target;
    }

    @PrePersist
    void setCreationTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void setUpdateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getFailureCode() { return failureCode; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
