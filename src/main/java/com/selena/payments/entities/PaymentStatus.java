package com.selena.payments.entities;

public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case PENDING -> target == SUCCEEDED || target == FAILED || target == CANCELLED;
            case SUCCEEDED -> target == REFUNDED;
            case FAILED, CANCELLED, REFUNDED -> false;
        };
    }
}
