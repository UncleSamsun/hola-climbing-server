package com.holaclimbing.server.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * `analysis:progress` 구독자. 진행 이벤트를 받으면 상태 저장소를 갱신하고 SSE로 fan-out한다.
 * 같은 Spring 클러스터 내 모든 인스턴스가 동일 메시지를 수신하므로, 각 인스턴스가 보유한 SSE
 * emitter에 push할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisProgressListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final AnalysisStatusStore statusStore;
    private final VideoAnalysisSseService sseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            AnalysisProgress progress = objectMapper.readValue(message.getBody(), AnalysisProgress.class);
            statusStore.save(progress);
            sseService.broadcast(progress);
        } catch (Exception e) {
            log.warn("진행 이벤트 처리 실패: {}", e.getMessage());
        }
    }
}
