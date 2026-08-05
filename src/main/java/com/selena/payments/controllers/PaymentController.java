package com.selena.payments.controllers;

import com.selena.payments.dto.CreatePaymentRequest;
import com.selena.payments.dto.PaymentResponse;
import com.selena.payments.services.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request,
                                         @RequestHeader("Idempotency-Key") UUID idempotencyKey) {
        return paymentService.createPayment(request, idempotencyKey);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        return paymentService.findById(id);
    }

    @GetMapping
    public Page<PaymentResponse> getPaymentsByBookingId(@RequestParam @Positive Long bookingId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(defaultValue = "createdAt") String sortBy,
                                                        @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return paymentService.findByBookingId(bookingId, pageable);
    }

    @PostMapping("/{id}/confirm")
    public PaymentResponse confirmPayment(@PathVariable UUID id,
                                          @RequestParam String providerTransactionId) {
        return paymentService.confirmPayment(id, providerTransactionId);
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancelPayment(@PathVariable UUID id) {
        return paymentService.cancelPayment(id);
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable UUID id) {
        return paymentService.refundPayment(id);
    }
}
