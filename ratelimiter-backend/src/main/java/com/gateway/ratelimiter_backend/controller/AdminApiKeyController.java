package com.gateway.ratelimiter_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.ratelimiter_backend.dto.ApiKeyCreatedResponse;
import com.gateway.ratelimiter_backend.dto.ApiKeyInfo;
import com.gateway.ratelimiter_backend.service.ApiKeyService;

@RestController
@RequestMapping("/admin")
public class AdminApiKeyController {

    private final ApiKeyService apiKeyService;

    public AdminApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/clients/{clientId}/keys")
    public ResponseEntity<ApiKeyCreatedResponse> create(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "90") int expiresInDays) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.createForClient(clientId, expiresInDays));
    }

    @GetMapping("/clients/{clientId}/keys")
    public List<ApiKeyInfo> list(@PathVariable Long clientId) {
        return apiKeyService.listForClient(clientId);
    }

    @PostMapping("/keys/{keyId}/rotate")
    public ApiKeyCreatedResponse rotate(@PathVariable Long keyId) {
        return apiKeyService.rotate(keyId);
    }

    @DeleteMapping("/keys/{keyId}")
    public ResponseEntity<Void> revoke(@PathVariable Long keyId) {
        apiKeyService.revoke(keyId);
        return ResponseEntity.noContent().build();
    }
}