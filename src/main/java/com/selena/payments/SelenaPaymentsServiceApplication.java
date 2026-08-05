package com.selena.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SelenaPaymentsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelenaPaymentsServiceApplication.class, args);
    }
}
