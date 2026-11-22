package com.gateway.ratelimiter_backend.dto;

import java.time.Instant;

public record ApiKeyCreatedResponse(
        Long keyId,
        Long clientId,
        String apiKey,
        String keyPrefix,
        Instant expiresAt,
        String warning) {
}