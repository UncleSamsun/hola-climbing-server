package com.holaclimbing.server.domain.video.dto.response;

/**
 * 영상 분석 진행 상태 응답 (F-02-09).
 * status: pending | done | failed
 */
public record VideoStatusResponse(
        Long videoId,
        String status
) {
}
