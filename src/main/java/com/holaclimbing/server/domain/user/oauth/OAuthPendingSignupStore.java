package com.holaclimbing.server.domain.user.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class OAuthPendingSignupStore {

    private static final String PREFIX = "auth:oauth:signup:";

    private final OAuthProperties properties;
    private final OAuthJsonRedisStore<OAuthPendingSignup> store;

    public OAuthPendingSignupStore(StringRedisTemplate redis, ObjectMapper objectMapper, OAuthProperties properties) {
        this.properties = properties;
        this.store = new OAuthJsonRedisStore<>(PREFIX, OAuthPendingSignup.class, redis, objectMapper) {
        };
    }

    public String issue(OAuthPendingSignup pendingSignup) {
        return store.issue(pendingSignup, Duration.ofMinutes(properties.pendingSignupTtlMinutes()));
    }

    public Optional<OAuthPendingSignup> consume(String token) {
        return store.consume(token);
    }
}
