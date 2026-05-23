package com.holaclimbing.server.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 영상별 최신 분석 상태 캐시. SSE 재연결·폴링·FCM이 동일한 상태 소스를 참조하도록 한다.
 * 7일 TTL — 완료 후에도 한동안 클라이언트가 마지막 상태를 조회할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisStatusStore {

    private static final String KEY_PREFIX = "analysis:status:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /** 최신 진행 상태를 저장한다. */
    public void save(AnalysisProgress progress) {
        try {
            redis.opsForValue().set(KEY_PREFIX + progress.videoId(),
                    objectMapper.writeValueAsString(progress), TTL);
        } catch (Exception e) {
            log.warn("분석 상태 저장 실패 — videoId={}: {}", progress.videoId(), e.getMessage());
        }
    }

    /** 저장된 최신 상태를 조회. SSE 연결 직후 1회 replay 용도. */
    public Optional<AnalysisProgress> find(Long videoId) {
        String json = redis.opsForValue().get(KEY_PREFIX + videoId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, AnalysisProgress.class));
        } catch (Exception e) {
            log.warn("분석 상태 역직렬화 실패 — videoId={}: {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }
}
