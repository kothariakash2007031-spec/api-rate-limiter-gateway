package com.gateway.gateway_service.filter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import com.gateway.gateway_service.model.KeyInfo;

import reactor.core.publisher.Mono;

@Component
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    public static final String KEY_INFO_ATTR = "keyInfo";

    private final WebClient webClient;

    public ApiKeyAuthFilter(@Value("${backend.base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing API key");
        }
        return lookup(apiKey).flatMap(result -> {
            if (result.isEmpty()) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or revoked API key");
            }
            exchange.getAttributes().put(KEY_INFO_ATTR, result.get());
            return chain.filter(exchange);
        });
    }

    private Mono<Optional<KeyInfo>> lookup(String apiKey) {
        return webClient.post()
                .uri("/internal/keys/validate")
                .header("X-API-Key", apiKey)
                .retrieve()
                .bodyToMono(KeyInfo.class)
                .map(Optional::of)
                .onErrorResume(WebClientResponseException.Unauthorized.class,
                        e -> Mono.just(Optional.empty()));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}