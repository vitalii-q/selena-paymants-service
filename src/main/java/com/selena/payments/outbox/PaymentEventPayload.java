package com.selena.payments.outbox;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentEventPayload(
        UUID paymentId,
        Long bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentEventType eventType,
        LocalDateTime occurredAt
) {
}
