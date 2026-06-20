package com.holaclimbing.server.domain.user.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class OAuthStateStore {

    private static final String PREFIX = "auth:oauth:state:";

    private final OAuthProperties properties;
    private final OAuthJsonRedisStore<OAuthState> store;

    public OAuthStateStore(StringRedisTemplate redis, ObjectMapper objectMapper, OAuthProperties properties) {
        this.properties = properties;
        this.store = new OAuthJsonRedisStore<>(PREFIX, OAuthState.class, redis, objectMapper) {
        };
    }

    public String issue(OAuthState state) {
        return store.issue(state, Duration.ofMinutes(properties.stateTtlMinutes()));
    }

    public Optional<OAuthState> consume(String token) {
        return store.consume(token);
    }
}
