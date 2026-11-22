package com.gateway.ratelimiter_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.ratelimiter_backend.dto.KeyValidationResponse;
import com.gateway.ratelimiter_backend.service.ApiKeyService;

@RestController
@RequestMapping("/internal/keys")
public class InternalKeyController {

    private final ApiKeyService apiKeyService;

    public InternalKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/validate")
    public ResponseEntity<KeyValidationResponse> validate(@RequestHeader("X-API-Key") String apiKey) {
        return apiKeyService.validateForGateway(apiKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}