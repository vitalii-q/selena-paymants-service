package com.selena.payments.controllers;

import com.selena.payments.dto.CreatePaymentRequest;
import com.selena.payments.dto.PaymentResponse;
import com.selena.payments.exceptions.BusinessException;
import com.selena.payments.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
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
                                         @RequestHeader("Idempotency-Key") UUID idempotencyKey, // Provide a uniquea unique Header "Idempotency-Key"
                                         HttpServletRequest servletRequest) {
        UUID authenticatedUserId = resolveAuthenticatedUserId(servletRequest);
        if (!authenticatedUserId.equals(request.getUserId())) {
            throw new BusinessException("PAYMENT_ACCESS_DENIED", "Authenticated user does not match payment owner");
        }
        return paymentService.createPayment(request, idempotencyKey, authenticatedUserId);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id, HttpServletRequest servletRequest) {
        return paymentService.findById(id, resolveAuthenticatedUserId(servletRequest));
    }

    @GetMapping
    public Page<PaymentResponse> getPaymentsByBookingId(@RequestParam @Positive Long bookingId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(defaultValue = "createdAt") String sortBy,
                                                        @RequestParam(defaultValue = "desc") String direction,
                                                        HttpServletRequest servletRequest) {
        UUID authenticatedUserId = resolveAuthenticatedUserId(servletRequest);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return paymentService.findByBookingId(bookingId, authenticatedUserId, pageable);
    }

    @PostMapping("/{id}/confirm")
    public PaymentResponse confirmPayment(@PathVariable UUID id,
                                          @RequestParam String providerTransactionId,
                                          HttpServletRequest servletRequest) {
        return paymentService.confirmPayment(id, providerTransactionId, resolveAuthenticatedUserId(servletRequest));
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancelPayment(@PathVariable UUID id, HttpServletRequest servletRequest) {
        return paymentService.cancelPayment(id, resolveAuthenticatedUserId(servletRequest));
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable UUID id, HttpServletRequest servletRequest) {
        return paymentService.refundPayment(id, resolveAuthenticatedUserId(servletRequest));
    }

    private UUID resolveAuthenticatedUserId(HttpServletRequest servletRequest) {
        // AUTH DISABLED TEMPORARILY     TODO: ACTIVATE AUTHENTICATION
        return UUID.fromString("4a8f9c86-8788-482b-8f68-eed530246277");

        /*String authenticatedUserId = servletRequest.getHeader("X-Authenticated-UserId");
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            authenticatedUserId = servletRequest.getHeader("X-Authenticated-Userid");
        }
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new BusinessException("AUTHENTICATION_REQUIRED", "Gateway user id header is required");
        }
        try {
            return UUID.fromString(authenticatedUserId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_AUTH_USER_ID", "Authenticated user id header must be a UUID");
        }*/
    }
}
