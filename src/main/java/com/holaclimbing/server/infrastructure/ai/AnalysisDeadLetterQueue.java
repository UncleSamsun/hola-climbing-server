package com.holaclimbing.server.infrastructure.ai;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 분석 진행 이벤트 처리 실패를 담는 dead-letter queue.
 *
 * <p>{@link AnalysisProgressListener}가 Redis Pub/Sub로 받은 progress 메시지를 처리하다
 * 역직렬화·DB 오류 등으로 실패하면, 메시지를 그냥 버리지 않고 Redis Stream
 * {@code analysis:dlq}에 원본 페이로드·사유와 함께 적재한다. 운영자가 나중에 조사·재처리할 수 있고,
 * {@code analysis.dlq.total} 카운터로 Grafana에서 추이를 본다.</p>
 */
@Slf4j
@Component
public class AnalysisDeadLetterQueue {

    public static final String DLQ_KEY = "analysis:dlq";

    private final StringRedisTemplate redis;
    private final MeterRegistry meterRegistry;

    public AnalysisDeadLetterQueue(StringRedisTemplate redis, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 처리 불가능한 진행 이벤트를 dead-letter로 기록한다.
     *
     * @param reason     실패 사유 (예외 메시지)
     * @param rawPayload 원본 메시지 본문 (재처리·조사용)
     */
    public void record(String reason, String rawPayload) {
        meterRegistry.counter("analysis.dlq.total").increment();
        try {
            Map<String, String> entry = new HashMap<>();
            entry.put("reason", reason == null ? "" : reason);
            entry.put("payload", rawPayload == null ? "" : rawPayload);
            entry.put("deadAt", Instant.now().toString());
            redis.opsForStream().add(MapRecord.create(DLQ_KEY, entry));
            log.warn("진행 이벤트 dead-letter 적재 — reason={}", reason);
        } catch (Exception e) {
            // DLQ 적재마저 실패하면 (Redis 장애 등) 로그만 남긴다. 더 떨어질 곳이 없다.
            log.error("dead-letter 적재 실패 — reason={}, error={}", reason, e.getMessage());
        }
    }

    /** 현재 DLQ에 쌓인 항목 수 (운영·테스트 조회용). */
    public long size() {
        Long len = redis.opsForStream().size(DLQ_KEY);
        return len == null ? 0L : len;
    }
}
