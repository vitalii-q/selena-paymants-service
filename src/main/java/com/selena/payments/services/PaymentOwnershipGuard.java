package com.selena.payments.services;

import com.selena.payments.entities.PaymentEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentOwnershipGuard {

    public boolean isOwner(PaymentEntity payment, UUID userId) {
        return payment != null && payment.getUserId() != null && payment.getUserId().equals(userId);
    }

    public boolean isOwner(PaymentEntity payment, String userId) {
        return userId != null && isOwner(payment, UUID.fromString(userId));
    }
}
