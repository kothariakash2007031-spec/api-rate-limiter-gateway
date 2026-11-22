package com.gateway.ratelimiter_backend.dto;

public record KeyValidationResponse(
        Long keyId,
        Long clientId,
        String clientName,
        String planName,
        int requestsPerMinute,
        int dailyQuota) {
}