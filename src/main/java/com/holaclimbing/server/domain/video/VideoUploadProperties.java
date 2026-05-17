package com.holaclimbing.server.domain.video;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * application.yaml의 app.upload.* 키 매핑. 영상 업로드 제약 (NF-05).
 * - allowed-extensions: 허용 확장자 (mp4, mov, hevc)
 * - max-duration-seconds: 최대 영상 길이 (초)
 * - max-file-size-bytes: 최대 파일 크기 (바이트)
 */
@ConfigurationProperties(prefix = "app.upload")
public record VideoUploadProperties(
        List<String> allowedExtensions,
        int maxDurationSeconds,
        long maxFileSizeBytes
) {
}
