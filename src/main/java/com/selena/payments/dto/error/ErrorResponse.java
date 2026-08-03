package com.selena.payments.dto.error;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fields
) {
    public ErrorResponse(int status, String error, String message, Map<String, String> fields) {
        this(LocalDateTime.now(), status, error, message, fields);
    }
}
