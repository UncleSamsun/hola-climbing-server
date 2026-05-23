package com.holaclimbing.server.infrastructure.ai;

/**
 * 분석 요청 큐. 구현체를 갈아끼우면 Redis Streams ↔ GCP Pub/Sub 등 다른 백엔드로 전환할 수 있다.
 */
public interface AnalysisJobQueue {

    /** 작업을 큐에 적재한다. fire-and-forget — 호출자는 큐 적재 성공만 신경 쓰면 된다. */
    void enqueue(AnalysisJob job);
}
