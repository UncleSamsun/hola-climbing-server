package com.holaclimbing.server.infrastructure.ai;

/**
 * AI 워커(Python)로 보내는 분석 요청 페이로드.
 * 워커는 분석을 마친 뒤 callbackUrl(POST /api/analysis/videos/{videoId})로 결과를 전송한다.
 */
public record AnalysisDispatchPayload(
        Long videoId,
        String gcsPath,
        String callbackUrl
) {
}
