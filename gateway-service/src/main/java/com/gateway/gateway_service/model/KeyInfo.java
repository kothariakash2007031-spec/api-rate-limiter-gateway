package com.gateway.gateway_service.model;

public record KeyInfo(
        Long keyId,
        Long clientId,
        String clientName,
        String planName,
        int requestsPerMinute,
        int dailyQuota) {
}