package com.selena.payments.services;

import com.selena.payments.dto.CreatePaymentRequest;
import com.selena.payments.dto.PaymentResponse;
import com.selena.payments.entities.PaymentEntity;
import com.selena.payments.entities.PaymentStatus;
import com.selena.payments.exceptions.BusinessException;
import com.selena.payments.exceptions.IdempotencyConflictException;
import com.selena.payments.mappers.PaymentMapper;
import com.selena.payments.outbox.PaymentEventType;
import com.selena.payments.outbox.PaymentOutboxPublisher;
import com.selena.payments.providers.PaymentProvider;
import com.selena.payments.providers.PaymentProviderResult;
import com.selena.payments.repositories.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Run tests:
// mvn test

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProvider paymentProvider;

    @Mock
    private PaymentOutboxPublisher paymentOutboxPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentMapper paymentMapper;
    private CreatePaymentRequest request;
    private UUID idempotencyKey;

    @BeforeEach
    void setUp() {
        paymentMapper = new PaymentMapper();
        paymentService = new PaymentService(paymentRepository, paymentMapper, paymentProvider, paymentOutboxPublisher);
        request = validRequest();
        idempotencyKey = UUID.randomUUID();
    }

    @Test
    void createPayment_shouldCreatePaymentAndCallProvider() {
        when(paymentRepository.findByUserIdAndIdempotencyKey(request.getUserId(), idempotencyKey))
                .thenReturn(Optional.empty());
        when(paymentProvider.process(any())).thenReturn(new PaymentProviderResult(
                true,
                "stub_tx_123",
                null,
                null
        ));
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(request, idempotencyKey);

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentProvider).process(any());
        verify(paymentRepository).save(captor.capture());
        verify(paymentOutboxPublisher).publish(
                any(UUID.class),
                eq(request.getBookingId()),
                eq(request.getUserId()),
                eq(request.getAmount()),
                eq("EUR"),
                eq(PaymentEventType.PAYMENT_SUCCEEDED)
        );
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(captor.getValue().getCurrency()).isEqualTo("EUR");
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.providerTransactionId()).isEqualTo("stub_tx_123");
    }

    @Test
    void createPayment_shouldReturnExistingPaymentForSameIdempotencyKey() {
        PaymentEntity existing = payment("EUR");
        when(paymentRepository.findByUserIdAndIdempotencyKey(request.getUserId(), idempotencyKey))
                .thenReturn(Optional.of(existing));

        PaymentResponse response = paymentService.createPayment(request, idempotencyKey);

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(paymentProvider, never()).process(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPayment_shouldRejectChangedPayloadForExistingIdempotencyKey() {
        PaymentEntity existing = payment("EUR");
        request.setAmount(new BigDecimal("19.99"));
        when(paymentRepository.findByUserIdAndIdempotencyKey(request.getUserId(), idempotencyKey))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.createPayment(request, idempotencyKey))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void confirmPayment_shouldTransitionPendingPaymentToSucceeded() {
        PaymentEntity payment = payment("EUR");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.confirmPayment(payment.getId(), "provider-tx-1");

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.providerTransactionId()).isEqualTo("provider-tx-1");
    }

    @Test
    void cancelPayment_shouldTransitionPendingPaymentToCancelled() {
        PaymentEntity payment = payment("EUR");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.cancelPayment(payment.getId());

        assertThat(response.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void refundPayment_shouldTransitionSucceededPaymentToRefunded() {
        PaymentEntity payment = payment("EUR");
        payment.markSucceeded("provider-tx-1");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.refundPayment(payment.getId());

        assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void cancelPayment_shouldRejectInvalidTransition() {
        PaymentEntity payment = payment("EUR");
        payment.markSucceeded("provider-tx-1");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(payment.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot transition payment");
    }

    private CreatePaymentRequest validRequest() {
        CreatePaymentRequest value = new CreatePaymentRequest();
        value.setBookingId(42L);
        value.setUserId(UUID.randomUUID());
        value.setAmount(new BigDecimal("10.00"));
        value.setCurrency("eur");
        value.setPaymentToken("test-payment-token");
        value.setPaymentMethod("TEST_METHOD");
        return value;
    }

    private PaymentEntity payment(String currency) {
        return PaymentEntity.create(
                request.getBookingId(),
                request.getUserId(),
                request.getAmount(),
                currency,
                idempotencyKey
        );
    }
}
