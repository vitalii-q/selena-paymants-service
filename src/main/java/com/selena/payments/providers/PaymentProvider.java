package com.selena.payments.providers;

public interface PaymentProvider {

    PaymentProviderResult process(PaymentProviderRequest request);
}
