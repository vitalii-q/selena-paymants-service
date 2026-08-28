package com.selena.payments.services;

import com.selena.payments.dto.CreatePaymentRequest;
import com.selena.payments.dto.PaymentResponse;
import com.selena.payments.entities.PaymentEntity;
import com.selena.payments.exceptions.BusinessException;
import com.selena.payments.exceptions.IdempotencyConflictException;
import com.selena.payments.exceptions.PaymentNotFoundException;
import com.selena.payments.integration.PaymentIntegrationService;
import com.selena.payments.mappers.PaymentMapper;
import com.selena.payments.outbox.PaymentEventType;
import com.selena.payments.outbox.PaymentOutboxPublisher;
import com.selena.payments.providers.PaymentProvider;
import com.selena.payments.providers.PaymentProviderRequest;
import com.selena.payments.providers.PaymentProviderResult;
import com.selena.payments.repositories.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    private static final String SUPPORTED_CURRENCY = "EUR";

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentProvider paymentProvider;
    private final PaymentOutboxPublisher paymentOutboxPublisher;
    private final PaymentIntegrationService paymentIntegrationService;

    public PaymentService(PaymentRepository paymentRepository, PaymentMapper paymentMapper,
                          PaymentProvider paymentProvider, PaymentOutboxPublisher paymentOutboxPublisher,
                          PaymentIntegrationService paymentIntegrationService) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.paymentProvider = paymentProvider;
        this.paymentOutboxPublisher = paymentOutboxPublisher;
        this.paymentIntegrationService = paymentIntegrationService;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, UUID idempotencyKey, UUID authenticatedUserId) {
        validateCreateRequest(request, idempotencyKey);
        ensureOwner(request.getUserId(), authenticatedUserId);
        String currency = request.getCurrency().toUpperCase(Locale.ROOT);

        LOGGER.info("Creating payment requested for bookingId={} with idempotencyKey={}", request.getBookingId(), idempotencyKey);

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
                    PaymentProviderResult providerResult = paymentProvider.process(
                            new PaymentProviderRequest(
                                    request.getPaymentToken(),
                                    request.getPaymentMethod(),
                                    request.getAmount(),
                                    currency
                            )
                    );
                    if (providerResult.success()) {
                        payment.markSucceeded(providerResult.providerTransactionId());
                        paymentOutboxPublisher.publish(
                                payment.getId(),
                                payment.getBookingId(),
                                payment.getUserId(),
                                payment.getAmount(),
                                payment.getCurrency(),
                                PaymentEventType.PAYMENT_SUCCEEDED
                        );
                        paymentIntegrationService.confirmBooking(payment.getBookingId(), "CONFIRMED");
                    } else {
                        payment.markFailed(providerResult.failureCode(), providerResult.failureReason());
                        paymentOutboxPublisher.publish(
                                payment.getId(),
                                payment.getBookingId(),
                                payment.getUserId(),
                                payment.getAmount(),
                                payment.getCurrency(),
                                PaymentEventType.PAYMENT_FAILED
                        );
                        paymentIntegrationService.confirmBooking(payment.getBookingId(), "CANCELLED");
                    }
                    PaymentResponse paymentResponse = paymentMapper.toResponse(paymentRepository.save(payment));
                    LOGGER.info("Payment created paymentId={} bookingId={} status={}", payment.getId(), payment.getBookingId(), payment.getStatus());
                    return paymentResponse;
                });
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID paymentId, UUID authenticatedUserId) {
        PaymentEntity payment = findPayment(paymentId);
        ensureOwner(payment.getUserId(), authenticatedUserId);
        LOGGER.info("Payment lookup paymentId={} bookingId={}", paymentId, payment.getBookingId());
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByBookingId(Long bookingId, UUID authenticatedUserId, Pageable pageable) {
        Page<PaymentEntity> payments = paymentRepository.findAllByBookingIdAndUserId(bookingId, authenticatedUserId, pageable);
        LOGGER.info("Payment collection requested for bookingId={} total={}", bookingId, payments.getTotalElements());
        return payments.map(paymentMapper::toResponse);
    }

    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId, String providerTransactionId, UUID authenticatedUserId) {
        if (!StringUtils.hasText(providerTransactionId)) {
            throw new BusinessException("INVALID_PROVIDER_TRANSACTION", "Provider transaction id must be provided");
        }

        PaymentEntity payment = findPayment(paymentId);
        ensureOwner(payment.getUserId(), authenticatedUserId);
        payment.markSucceeded(providerTransactionId);
        paymentOutboxPublisher.publish(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                PaymentEventType.PAYMENT_SUCCEEDED
        );
        paymentIntegrationService.confirmBooking(payment.getBookingId(), "CONFIRMED");
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse failPayment(UUID paymentId, String failureCode, String failureReason, UUID authenticatedUserId) {
        if (!StringUtils.hasText(failureCode) || !StringUtils.hasText(failureReason)) {
            throw new BusinessException("INVALID_FAILURE_DETAILS", "Failure code and reason must be provided");
        }

        PaymentEntity payment = findPayment(paymentId);
        ensureOwner(payment.getUserId(), authenticatedUserId);
        payment.markFailed(failureCode, failureReason);
        paymentOutboxPublisher.publish(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                PaymentEventType.PAYMENT_FAILED
        );
        paymentIntegrationService.confirmBooking(payment.getBookingId(), "CANCELLED");
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId, UUID authenticatedUserId) {
        PaymentEntity payment = findPayment(paymentId);
        ensureOwner(payment.getUserId(), authenticatedUserId);
        payment.cancel();
        LOGGER.info("Payment cancelled paymentId={} bookingId={}", paymentId, payment.getBookingId());
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, UUID authenticatedUserId) {
        PaymentEntity payment = findPayment(paymentId);
        ensureOwner(payment.getUserId(), authenticatedUserId);
        payment.refund();
        paymentOutboxPublisher.publish(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                PaymentEventType.PAYMENT_REFUNDED
        );
        paymentIntegrationService.confirmBooking(payment.getBookingId(), "CANCELLED");
        return paymentMapper.toResponse(payment);
    }

    private PaymentEntity findPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private void ensureOwner(UUID ownerId, UUID authenticatedUserId) {
        // AUTH DISABLED TEMPORARILY     TODO: ACTIVATE AUTHENTICATION
        /*if (authenticatedUserId == null || !authenticatedUserId.equals(ownerId)) {
            throw new BusinessException("PAYMENT_ACCESS_DENIED", "Authenticated user does not match payment owner");
        }*/
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
                || request.getAmount() == null || !StringUtils.hasText(request.getCurrency())
                || !StringUtils.hasText(request.getPaymentToken()) || !StringUtils.hasText(request.getPaymentMethod())) {
            throw new BusinessException("INVALID_PAYMENT_REQUEST", "Booking, user, amount, currency, payment token and payment method are required");
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
