package com.holaclimbing.server.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로그아웃된 JWT의 jti를 Redis에 등록·조회한다.
 * 키 TTL을 토큰 잔여 수명으로 두어, 토큰이 만료되면 블랙리스트 항목도 자동 소멸한다.
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    /** jti를 잔여 수명(ttl) 동안 블랙리스트에 등록. */
    public void blacklist(String jti, Duration ttl) {
        if (jti == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redis.opsForValue().set(PREFIX + jti, "1", ttl);
    }

    /** jti가 블랙리스트에 있는지 여부. */
    public boolean contains(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
