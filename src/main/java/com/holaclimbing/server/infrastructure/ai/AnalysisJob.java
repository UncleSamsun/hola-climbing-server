package com.holaclimbing.server.infrastructure.ai;

/**
 * Spring → Python으로 보내는 분석 요청 메시지.
 * Redis Stream `analysis:requests`에 적재된다.
 */
public record AnalysisJob(
        Long videoId,
        String gcsPath,
        String callbackUrl
) {
}
