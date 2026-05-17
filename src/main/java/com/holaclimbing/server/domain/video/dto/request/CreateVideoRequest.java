package com.holaclimbing.server.domain.video.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 영상 등록 요청 (메타데이터).
 * gcsPath는 업로드된 파일 경로 — 실제 업로드는 2단계(GCS 연동)에서 처리한다.
 */
public record CreateVideoRequest(
        Long gymId,
        @Size(max = 100) String title,
        String description,
        @Size(max = 20) String grade,
        @NotBlank @Size(max = 500) String gcsPath,
        @Size(max = 500) String thumbnailPath,
        @Positive Integer durationSeconds,
        Boolean isPublic
) {
}
