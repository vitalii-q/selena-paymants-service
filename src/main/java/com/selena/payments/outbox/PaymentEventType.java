package com.selena.payments.outbox;

public enum PaymentEventType {
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED
}
