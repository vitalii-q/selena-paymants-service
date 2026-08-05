package com.selena.payments.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payments.provider.stub")
public class StubPaymentProviderProperties {

    private StubPaymentProviderMode mode = StubPaymentProviderMode.SUCCESS;

    public StubPaymentProviderMode getMode() {
        return mode;
    }

    public void setMode(StubPaymentProviderMode mode) {
        this.mode = mode;
    }
}
