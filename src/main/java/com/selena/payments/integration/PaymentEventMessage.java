package com.selena.payments.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentEventMessage(
        UUID paymentId,
        Long bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String eventType,
        LocalDateTime occurredAt
) {
}
