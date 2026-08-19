package com.selena.payments.integration;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentIntegrationService {

    private final RestTemplate restTemplate;

    public PaymentIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void confirmBooking(Long bookingId, String status) {
        String url = "http://localhost:9066/api/bookings/" + bookingId + "/status";
        restTemplate.patchForObject(url, new BookingStatusUpdateRequest(status), Void.class);
    }

    public static class BookingStatusUpdateRequest {
        private String status;

        public BookingStatusUpdateRequest() {
        }

        public BookingStatusUpdateRequest(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
