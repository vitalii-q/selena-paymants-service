package com.selena.payments.repositories;

import com.selena.payments.entities.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);

    Page<PaymentEntity> findAllByBookingIdAndUserId(Long bookingId, UUID userId, Pageable pageable);
}
