package com.selena.payments.dto;

import com.selena.payments.entities.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        Long bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String providerTransactionId,
        String failureCode,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
