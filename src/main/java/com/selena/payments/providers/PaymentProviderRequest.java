package com.selena.payments.providers;

import java.math.BigDecimal;

public record PaymentProviderRequest(
        String paymentToken,
        String paymentMethod,
        BigDecimal amount,
        String currency
) {
}
