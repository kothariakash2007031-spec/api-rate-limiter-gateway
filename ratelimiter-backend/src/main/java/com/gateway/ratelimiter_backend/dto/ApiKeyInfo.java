package com.gateway.ratelimiter_backend.dto;

import java.time.Instant;

public record ApiKeyInfo(
        Long keyId,
        String keyPrefix,
        String status,
        Instant createdAt,
        Instant expiresAt) {
}