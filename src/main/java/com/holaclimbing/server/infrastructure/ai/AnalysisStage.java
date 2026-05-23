package com.holaclimbing.server.infrastructure.ai;

/**
 * AI 영상 분석 진행 단계. 진행률(%) 대신 단계 enum으로 상태를 표현한다.
 */
public enum AnalysisStage {
    /** Spring이 분석 요청을 큐(analysis:requests)에 적재한 직후. */
    QUEUED,
    /** Python 워커가 작업을 픽업해 분석을 진행 중. */
    PROCESSING,
    /** 분석 결과 수신 및 저장 완료. */
    COMPLETED,
    /** 분석 실패. message에 사유가 담긴다. */
    FAILED
}
