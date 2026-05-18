package com.holaclimbing.server.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 영상 등록 시 AI 워커(Python)에 분석을 요청하는 아웃바운드 디스패처.
 * fire-and-forget — 워커 호출 실패는 영상 등록에 영향을 주지 않으며,
 * 실패 시 영상은 pending으로 남아 재시도 여지를 둔다.
 * ai.analysis-url이 비어 있으면(개발 환경 등) 디스패치를 건너뛴다.
 */
@Slf4j
@Component
public class AnalysisDispatcher {

    private final RestClient restClient = RestClient.create();
    private final String analysisUrl;
    private final String baseUrl;

    public AnalysisDispatcher(@Value("${ai.analysis-url:}") String analysisUrl,
                              @Value("${app.base-url}") String baseUrl) {
        this.analysisUrl = analysisUrl;
        this.baseUrl = baseUrl;
    }

    /** 영상 분석 요청을 워커로 전송한다. 비동기 — 호출자는 결과를 기다리지 않는다. */
    @Async
    public void dispatch(Long videoId, String gcsPath) {
        if (analysisUrl == null || analysisUrl.isBlank()) {
            log.info("AI 분석 디스패치 건너뜀 (ai.analysis-url 미설정) — videoId={}", videoId);
            return;
        }
        AnalysisDispatchPayload payload = new AnalysisDispatchPayload(
                videoId, gcsPath, baseUrl + "/api/analysis/videos/" + videoId);
        try {
            restClient.post()
                    .uri(analysisUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("AI 분석 디스패치 완료 — videoId={}", videoId);
        } catch (Exception e) {
            log.warn("AI 분석 디스패치 실패 — videoId={} (영상은 pending 유지): {}", videoId, e.getMessage());
        }
    }
}
