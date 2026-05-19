package com.holaclimbing.server.domain.gym.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 암장 사진 업로드 요청. gcsPath는 업로드 URL 발급으로 미리 올린 GCS 객체 경로.
 */
public record CreateGymPhotoRequest(
        @NotBlank @Size(max = 500) String gcsPath,
        @Size(max = 200) String caption,
        Integer displayOrder
) {
}
