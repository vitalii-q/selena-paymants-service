package com.selena.payments.providers;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StubPaymentProvider implements PaymentProvider {

    private final StubPaymentProviderProperties properties;

    public StubPaymentProvider(StubPaymentProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentProviderResult process(PaymentProviderRequest request) {
        if (request == null || request.paymentToken() == null || request.paymentMethod() == null) {
            return new PaymentProviderResult(false, null, "INVALID_PROVIDER_REQUEST", "Payment token and method are required");
        }

        if (properties.getMode() == StubPaymentProviderMode.FAILURE) {
            return new PaymentProviderResult(
                    false,
                    null,
                    "STUB_PROVIDER_FAILURE",
                    "Stub provider rejected the test payment token"
            );
        }

        return new PaymentProviderResult(
                true,
                "stub_" + UUID.randomUUID(),
                null,
                null
        );
    }
}
