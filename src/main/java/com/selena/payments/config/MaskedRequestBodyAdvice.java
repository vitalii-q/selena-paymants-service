package com.selena.payments.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class MaskedRequestBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public MaskedRequestBodyAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        Map<String, Object> masked = new HashMap<>();
        if (body instanceof Map<?, ?> map) {
            map.forEach((key, value) -> masked.put(String.valueOf(key), sanitizeValue(String.valueOf(key), value)));
            return masked;
        }

        try {
            Map<String, Object> source = objectMapper.convertValue(body, new TypeReference<Map<String, Object>>() {
            });
            source.forEach((key, value) -> masked.put(key, sanitizeValue(key, value)));
            return masked;
        } catch (IllegalArgumentException ex) {
            return body;
        }
    }

    private Object sanitizeValue(String key, Object value) {
        if (key == null) {
            return value;
        }

        String sanitizedKey = key.toLowerCase();
        if (sanitizedKey.contains("token") || sanitizedKey.contains("secret") || sanitizedKey.contains("password")
                || sanitizedKey.contains("card") || sanitizedKey.contains("pan")) {
            return "***MASKED***";
        }

        return value;
    }
}
