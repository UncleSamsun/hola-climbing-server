package com.holaclimbing.server.domain.user.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaclimbing.server.domain.user.dto.response.OAuthLoginResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class OAuthResultStore {

    private static final String PREFIX = "auth:oauth:result:";

    private final OAuthProperties properties;
    private final OAuthJsonRedisStore<OAuthLoginResponse> store;

    public OAuthResultStore(StringRedisTemplate redis, ObjectMapper objectMapper, OAuthProperties properties) {
        this.properties = properties;
        this.store = new OAuthJsonRedisStore<>(PREFIX, OAuthLoginResponse.class, redis, objectMapper) {
        };
    }

    public String issue(OAuthLoginResponse response) {
        return store.issue(response, Duration.ofMinutes(properties.resultTtlMinutes()));
    }

    public Optional<OAuthLoginResponse> consume(String code) {
        return store.consume(code);
    }
}
