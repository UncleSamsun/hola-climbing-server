package com.holaclimbing.server.infrastructure.gcs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 gcs.* 키 매핑.
 * - bucket: 영상 버킷 이름
 * - upload-prefix: 업로드 객체 경로 prefix (예: videos/uploads)
 * - signed-url-minutes: Signed URL 유효시간 (분)
 */
@ConfigurationProperties(prefix = "gcs")
public record GcsProperties(
        @NotBlank String bucket,
        @NotBlank String uploadPrefix,
        @Positive int signedUrlMinutes
) {
}
