package com.selena.payments.providers;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class StubPaymentProviderTest {

    @Test
    void shouldReturnSuccessAndGenerateProviderTransactionId() {
        StubPaymentProvider provider = new StubPaymentProvider(new StubPaymentProviderProperties());

        PaymentProviderResult result = provider.process(
                new PaymentProviderRequest("test-token", "TEST_METHOD", new BigDecimal("12.34"), "EUR")
        );

        assertTrue(result.success());
        assertNotNull(result.providerTransactionId());
        assertTrue(result.providerTransactionId().startsWith("stub_"));
        assertNull(result.failureCode());
        assertNull(result.failureReason());
    }

    @Test
    void shouldReturnDeterministicFailureWhenConfigured() {
        StubPaymentProviderProperties properties = new StubPaymentProviderProperties();
        properties.setMode(StubPaymentProviderMode.FAILURE);

        StubPaymentProvider provider = new StubPaymentProvider(properties);

        PaymentProviderResult result = provider.process(
                new PaymentProviderRequest("test-token", "TEST_METHOD", new BigDecimal("12.34"), "EUR")
        );

        assertFalse(result.success());
        assertEquals("STUB_PROVIDER_FAILURE", result.failureCode());
        assertEquals("Stub provider rejected the test payment token", result.failureReason());
        assertNull(result.providerTransactionId());
    }
}
