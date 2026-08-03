package com.selena.payments.exceptions;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment with id " + paymentId + " was not found");
    }
}
