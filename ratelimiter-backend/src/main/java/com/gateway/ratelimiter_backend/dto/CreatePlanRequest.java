package com.gateway.ratelimiter_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreatePlanRequest(
        @NotBlank String name,
        @Min(1) int requestsPerMinute,
        @Min(1) int dailyQuota) {
}