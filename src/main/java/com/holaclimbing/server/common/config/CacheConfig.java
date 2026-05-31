package com.holaclimbing.server.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 기반 Spring Cache.
 *
 * <p>캐시별 TTL을 분리한다:</p>
 * <ul>
 *   <li>{@code gcsReadUrl} (5분) — 영상 재생용 Signed URL. 발급 TTL(15분)보다 짧게 잡아
 *       만료 직전 URL이 캐시에서 나가지 않게 한다. 피드 1건당 1회 서명 비용을 크게 줄인다.</li>
 *   <li>{@code activeTerms} (10분) — 약관 목록. 거의 안 바뀌고 회원가입 폼마다 조회된다.</li>
 * </ul>
 *
 * <p>키 직렬화는 문자열, 값은 JSON. 캐시 미스 시 원 메서드를 호출한다.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_GCS_READ_URL = "gcsReadUrl";
    public static final String CACHE_ACTIVE_TERMS = "activeTerms";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 무인자 생성자는 캐시 값 역직렬화를 위한 기본 타입 정보를 자동 구성한다 (record·List 안전).
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .prefixCacheNameWith("cache:");

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                CACHE_GCS_READ_URL, base.entryTtl(Duration.ofMinutes(5)),
                CACHE_ACTIVE_TERMS, base.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(1)))
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
