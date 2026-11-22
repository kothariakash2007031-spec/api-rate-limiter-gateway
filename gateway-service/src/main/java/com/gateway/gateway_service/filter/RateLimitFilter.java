package com.gateway.gateway_service.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.gateway.gateway_service.model.KeyInfo;

import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final long WINDOW_SECONDS = 60;

    private final ReactiveStringRedisTemplate redis;

    public RateLimitFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        KeyInfo info = exchange.getAttribute(ApiKeyAuthFilter.KEY_INFO_ATTR);
        if (info == null) {
            return chain.filter(exchange);
        }

        long now = Instant.now().getEpochSecond();
        long window = now / WINDOW_SECONDS;
        long retryAfter = WINDOW_SECONDS - (now % WINDOW_SECONDS);
        String redisKey = "rl:" + info.clientId() + ":" + window;
        int limit = info.requestsPerMinute();

        return redis.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    Mono<Boolean> setTtl = (count == 1)
                            ? redis.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS))
                            : Mono.just(true);
                    return setTtl.thenReturn(count);
                })
                .flatMap(count -> {
                    var headers = exchange.getResponse().getHeaders();
                    headers.add("X-RateLimit-Limit", String.valueOf(limit));
                    headers.add("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
                    if (count > limit) {
                        headers.add("Retry-After", String.valueOf(retryAfter));
                        return tooManyRequests(exchange, limit, retryAfter);
                    }
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, int limit, long retryAfter) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"error\":\"Rate limit exceeded\",\"limit\":" + limit
                + ",\"retryAfterSeconds\":" + retryAfter + "}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}