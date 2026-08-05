package com.selena.payments.providers;

public record PaymentProviderResult(
        boolean success,
        String providerTransactionId,
        String failureCode,
        String failureReason
) {
}
