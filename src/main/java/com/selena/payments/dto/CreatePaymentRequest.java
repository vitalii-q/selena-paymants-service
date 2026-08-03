package com.selena.payments.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class CreatePaymentRequest {

    @NotNull(message = "bookingId is required")
    @Positive(message = "bookingId must be greater than 0")
    private Long bookingId;

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "amount must have at most 2 fractional digits")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;

    public Long getBookingId() { return bookingId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
}
