package com.gateway.ratelimiter_backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import com.gateway.ratelimiter_backend.dto.KeyValidationResponse;
import com.gateway.ratelimiter_backend.entity.Plan;  

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gateway.ratelimiter_backend.dto.ApiKeyCreatedResponse;
import com.gateway.ratelimiter_backend.dto.ApiKeyInfo;
import com.gateway.ratelimiter_backend.entity.ApiKey;
import com.gateway.ratelimiter_backend.entity.Client;
import com.gateway.ratelimiter_backend.repository.ApiKeyRepository;
import com.gateway.ratelimiter_backend.security.ApiKeyGenerator;

@Service
public class ApiKeyService {

    private static final int DEFAULT_VALID_DAYS = 90;

    private final ApiKeyRepository apiKeyRepository;
    private final ClientService clientService;
    private final ApiKeyGenerator generator;

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         ClientService clientService,
                         ApiKeyGenerator generator) {
        this.apiKeyRepository = apiKeyRepository;
        this.clientService = clientService;
        this.generator = generator;
    }

    @Transactional
    public ApiKeyCreatedResponse createForClient(Long clientId, int expiresInDays) {
        Client client = clientService.findById(clientId);
        if (!"ACTIVE".equals(client.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client is not active");
        }
        return issue(client, expiresInDays);
    }

    @Transactional
    public ApiKeyCreatedResponse rotate(Long keyId) {
        ApiKey old = findKey(keyId);
        old.setStatus("REVOKED");
        return issue(old.getClient(), DEFAULT_VALID_DAYS);
    }

    @Transactional
    public void revoke(Long keyId) {
        findKey(keyId).setStatus("REVOKED");
    }

    @Transactional(readOnly = true)
    public List<ApiKeyInfo> listForClient(Long clientId) {
        clientService.findById(clientId);
        return apiKeyRepository.findByClientId(clientId).stream()
                .map(k -> new ApiKeyInfo(k.getId(), k.getKeyPrefix(), k.getStatus(),
                        k.getCreatedAt(), k.getExpiresAt()))
                .toList();
    }

    // Used by the gateway in Step 6
    @Transactional(readOnly = true)
    public Optional<ApiKey> validate(String rawKey) {
        return apiKeyRepository.findByKeyHash(generator.hash(rawKey))
                .filter(k -> "ACTIVE".equals(k.getStatus()))
                .filter(k -> k.getExpiresAt() == null || k.getExpiresAt().isAfter(Instant.now()))
                .filter(k -> "ACTIVE".equals(k.getClient().getStatus()));
    }
    @Transactional(readOnly = true)
    public Optional<KeyValidationResponse> validateForGateway(String rawKey) {
        return validate(rawKey)
                .filter(k -> k.getClient().getPlan() != null)
                .map(k -> {
                    Client c = k.getClient();
                    Plan p = c.getPlan();
                    return new KeyValidationResponse(k.getId(), c.getId(), c.getName(),
                            p.getName(), p.getRequestsPerMinute(), p.getDailyQuota());
                });
    }

    private ApiKeyCreatedResponse issue(Client client, int expiresInDays) {
        String rawKey = generator.generate();
        ApiKey key = new ApiKey();
        key.setClient(client);
        key.setKeyHash(generator.hash(rawKey));
        key.setKeyPrefix(rawKey.substring(0, 10));
        key.setExpiresAt(Instant.now().plus(expiresInDays, ChronoUnit.DAYS));
        ApiKey saved = apiKeyRepository.save(key);
        return new ApiKeyCreatedResponse(saved.getId(), client.getId(), rawKey,
                saved.getKeyPrefix(), saved.getExpiresAt(),
                "Copy this key now. It will not be shown again.");
    }

    private ApiKey findKey(Long keyId) {
        return apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Key not found"));
    }
}