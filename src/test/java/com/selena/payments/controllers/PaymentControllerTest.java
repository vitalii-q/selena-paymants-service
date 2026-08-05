package com.selena.payments.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selena.payments.dto.CreatePaymentRequest;
import com.selena.payments.dto.PaymentResponse;
import com.selena.payments.entities.PaymentStatus;
import com.selena.payments.services.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createPaymentShouldReturnCreated() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId(42L);
        request.setUserId(UUID.randomUUID());
        request.setAmount(new BigDecimal("10.00"));
        request.setCurrency("eur");
        request.setPaymentToken("test-token");
        request.setPaymentMethod("TEST_METHOD");

        when(paymentService.createPayment(any(CreatePaymentRequest.class), any(UUID.class)))
                .thenReturn(new PaymentResponse(
                        UUID.randomUUID(),
                        42L,
                        request.getUserId(),
                        request.getAmount(),
                        "EUR",
                        PaymentStatus.PENDING,
                        null,
                        null,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getPaymentByIdShouldReturnOk() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.findById(paymentId)).thenReturn(new PaymentResponse(
                paymentId,
                42L,
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                "EUR",
                PaymentStatus.PENDING,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        ));

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isOk());
    }
}
