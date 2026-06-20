package com.holaclimbing.server.domain.user.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

abstract class OAuthJsonRedisStore<T> {

    private final String prefix;
    private final Class<T> type;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    OAuthJsonRedisStore(String prefix, Class<T> type, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.prefix = prefix;
        this.type = type;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    String issue(T value, Duration ttl) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redis.opsForValue().set(prefix + token, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize OAuth value", e);
        }
        return token;
    }

    Optional<T> consume(String token) {
        String key = prefix + token;
        String value = redis.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        redis.delete(key);
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize OAuth value", e);
        }
    }
}
