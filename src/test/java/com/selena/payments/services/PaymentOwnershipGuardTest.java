package com.selena.payments.services;

import com.selena.payments.entities.PaymentEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentOwnershipGuardTest {

    @Test
    void shouldAllowAccessWhenAuthenticatedUserOwnsPayment() {
        UUID ownerId = UUID.randomUUID();
        PaymentEntity payment = PaymentEntity.create(1L, ownerId, BigDecimal.valueOf(10.00), "EUR", UUID.randomUUID());

        PaymentOwnershipGuard guard = new PaymentOwnershipGuard();

        assertTrue(guard.isOwner(payment, ownerId));
    }

    @Test
    void shouldRejectAccessWhenAuthenticatedUserDoesNotOwnPayment() {
        UUID ownerId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        PaymentEntity payment = PaymentEntity.create(1L, ownerId, BigDecimal.valueOf(10.00), "EUR", UUID.randomUUID());

        PaymentOwnershipGuard guard = new PaymentOwnershipGuard();

        assertFalse(guard.isOwner(payment, anotherUserId));
    }
}
