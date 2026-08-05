package com.selena.payments.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PaymentOutboxPublisher {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentOutboxPublisher(PaymentOutboxRepository paymentOutboxRepository, ObjectMapper objectMapper) {
        this.paymentOutboxRepository = paymentOutboxRepository;
        this.objectMapper = objectMapper;
    }

    public void publish(UUID paymentId, Long bookingId, UUID userId, BigDecimal amount,
                       String currency, PaymentEventType eventType) {
        PaymentEventPayload payload = new PaymentEventPayload(
                paymentId,
                bookingId,
                userId,
                amount,
                currency,
                eventType,
                LocalDateTime.now()
        );

        String serializedPayload;
        try {
            serializedPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payment outbox event", e);
        }

        PaymentOutboxEntity outboxEvent = new PaymentOutboxEntity(
                "Payment",
                paymentId,
                eventType.name(),
                serializedPayload,
                PaymentOutboxStatus.PENDING
        );

        paymentOutboxRepository.save(outboxEvent);
    }
}
