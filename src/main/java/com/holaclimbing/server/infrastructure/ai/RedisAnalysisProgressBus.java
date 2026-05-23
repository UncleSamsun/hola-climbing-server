package com.holaclimbing.server.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 기반 분석 진행 버스. `analysis:progress` 채널에 JSON 직렬화한 이벤트를 발행한다.
 * Spring 다중 인스턴스 환경에서도 모든 인스턴스가 같은 채널을 구독해 SSE fan-out이 가능하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAnalysisProgressBus implements AnalysisProgressBus {

    public static final String CHANNEL = "analysis:progress";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(AnalysisProgress progress) {
        try {
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(progress));
        } catch (Exception e) {
            log.warn("진행 이벤트 발행 실패 — videoId={}: {}", progress.videoId(), e.getMessage());
        }
    }
}
