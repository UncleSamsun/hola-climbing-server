package com.holaclimbing.server.domain.video.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 영상 부분 수정 요청. null인 필드는 변경하지 않는다(PATCH 시맨틱).
 */
public record UpdateVideoRequest(
        @Size(max = 100) String title,
        String description,
        Boolean isPublic
) {
}
