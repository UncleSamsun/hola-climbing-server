package com.holaclimbing.server.domain.video.dto.response;

import java.time.LocalDateTime;

/**
 * 업로드용 Signed URL 발급 응답.
 * - uploadUrl: 클라이언트가 PUT으로 직접 업로드할 GCS Signed URL
 * - gcsPath: 영상 등록(POST /api/videos) 시 그대로 넘길 객체 경로
 * - contentType: 업로드 PUT 요청에 동일하게 사용해야 하는 Content-Type
 * - expiresAt: Signed URL 만료 시각
 */
public record UploadUrlResponse(
        String uploadUrl,
        String gcsPath,
        String contentType,
        LocalDateTime expiresAt
) {
}
