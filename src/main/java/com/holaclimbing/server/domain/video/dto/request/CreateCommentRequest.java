package com.holaclimbing.server.domain.video.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성 요청. parentId가 있으면 대댓글(답글).
 */
public record CreateCommentRequest(
        @NotBlank @Size(max = 1000) String content,
        Long parentId
) {
}
