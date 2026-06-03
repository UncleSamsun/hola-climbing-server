package com.holaclimbing.server.domain.video.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 영상 등록 요청 (메타데이터).
 * objectPath는 업로드 URL 발급(POST /api/videos/upload-url)에서 받은 GCS 객체 경로.
 */
public record CreateVideoRequest(
        @NotNull
        Long gymId,
        @Size(max = 100) String title,
        String description,
        @NotNull
        Long gymGradeId,
        @NotBlank @Size(max = 500) String objectPath,
        @Size(max = 500) String thumbnailPath,
        @Positive Integer durationSeconds,
        @NotNull
        LocalDate recordedDate,
        Boolean isPublic
) {
}
