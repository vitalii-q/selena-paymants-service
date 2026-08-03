package com.selena.payments.services;

import com.selena.payments.dto.CreatePaymentRequest;
import com.selena.payments.dto.PaymentResponse;
import com.selena.payments.entities.PaymentEntity;
import com.selena.payments.exceptions.BusinessException;
import com.selena.payments.exceptions.IdempotencyConflictException;
import com.selena.payments.exceptions.PaymentNotFoundException;
import com.selena.payments.mappers.PaymentMapper;
import com.selena.payments.repositories.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private static final String SUPPORTED_CURRENCY = "EUR";

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public PaymentService(PaymentRepository paymentRepository, PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, UUID idempotencyKey) {
        validateCreateRequest(request, idempotencyKey);
        String currency = request.getCurrency().toUpperCase(Locale.ROOT);

        return paymentRepository.findByUserIdAndIdempotencyKey(request.getUserId(), idempotencyKey)
                .map(existing -> resolveIdempotentRequest(existing, request, currency))
                .orElseGet(() -> {
                    PaymentEntity payment = PaymentEntity.create(
                            request.getBookingId(),
                            request.getUserId(),
                            request.getAmount(),
                            currency,
                            idempotencyKey
                    );
                    return paymentMapper.toResponse(paymentRepository.save(payment));
                });
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID paymentId) {
        return paymentMapper.toResponse(findPayment(paymentId));
    }

    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId, String providerTransactionId) {
        if (!StringUtils.hasText(providerTransactionId)) {
            throw new BusinessException("INVALID_PROVIDER_TRANSACTION", "Provider transaction id must be provided");
        }

        PaymentEntity payment = findPayment(paymentId);
        payment.markSucceeded(providerTransactionId);
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse failPayment(UUID paymentId, String failureCode, String failureReason) {
        if (!StringUtils.hasText(failureCode) || !StringUtils.hasText(failureReason)) {
            throw new BusinessException("INVALID_FAILURE_DETAILS", "Failure code and reason must be provided");
        }

        PaymentEntity payment = findPayment(paymentId);
        payment.markFailed(failureCode, failureReason);
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId) {
        PaymentEntity payment = findPayment(paymentId);
        payment.cancel();
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {
        PaymentEntity payment = findPayment(paymentId);
        payment.refund();
        return paymentMapper.toResponse(payment);
    }

    private PaymentEntity findPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private PaymentResponse resolveIdempotentRequest(PaymentEntity existing, CreatePaymentRequest request,
                                                      String currency) {
        boolean matches = existing.getBookingId().equals(request.getBookingId())
                && existing.getAmount().compareTo(request.getAmount()) == 0
                && existing.getCurrency().equals(currency);
        if (!matches) {
            throw new IdempotencyConflictException();
        }
        return paymentMapper.toResponse(existing);
    }

    private void validateCreateRequest(CreatePaymentRequest request, UUID idempotencyKey) {
        if (request == null || request.getBookingId() == null || request.getUserId() == null
                || request.getAmount() == null || !StringUtils.hasText(request.getCurrency())) {
            throw new BusinessException("INVALID_PAYMENT_REQUEST", "Booking, user, amount and currency are required");
        }
        if (request.getBookingId() <= 0) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking id must be greater than 0");
        }
        if (request.getAmount().signum() <= 0 || request.getAmount().scale() > 2) {
            throw new BusinessException("INVALID_AMOUNT", "Amount must be positive and have at most 2 fractional digits");
        }
        if (!SUPPORTED_CURRENCY.equalsIgnoreCase(request.getCurrency())) {
            throw new BusinessException("UNSUPPORTED_CURRENCY", "Only EUR is supported");
        }
        if (idempotencyKey == null) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency key must be provided");
        }
    }
}
