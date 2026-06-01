package com.holaclimbing.server.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 이메일 인증 토큰 저장소. Redis 키 {@code auth:verify:{token}} = userId, TTL 24시간.
 *
 * <p>이전에는 토큰을 {@code users.email_verification_token} 컬럼에 평문·만료 없이 보관해,
 * 메일 캐시·백업에서 토큰이 새면 무한 재사용이 가능했다. Redis로 옮겨 24시간 후 자동 소멸하게 만든다.
 * DB 컬럼은 운영 디버깅용으로 함께 채우되, 실제 검증의 권위는 Redis 키 존재 여부다.</p>
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationTokenStore {

    private static final String PREFIX = "auth:verify:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    /** 토큰을 발급(또는 재발급). TTL 24h. */
    public void issue(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        redis.opsForValue().set(PREFIX + token, String.valueOf(userId), TTL);
    }

    /**
     * 토큰을 소비. 유효하면 userId를 반환하고 키를 삭제(1회용).
     * 만료/없음이면 empty.
     */
    public Optional<Long> consume(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String value = redis.opsForValue().getAndDelete(PREFIX + token);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
