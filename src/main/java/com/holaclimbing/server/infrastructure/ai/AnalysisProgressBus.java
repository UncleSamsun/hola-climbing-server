package com.holaclimbing.server.infrastructure.ai;

/**
 * 분석 진행 이벤트 발행 버스. Python이 직접 Redis Pub/Sub로 발행하는 경우가 일반적이지만,
 * Spring 내부에서도 (예: ingest 콜백 직후 COMPLETED) 동일 채널로 발행할 수 있다.
 */
public interface AnalysisProgressBus {

    /** 진행 이벤트를 게시한다. 구독자는 비동기로 수신한다. */
    void publish(AnalysisProgress progress);
}
