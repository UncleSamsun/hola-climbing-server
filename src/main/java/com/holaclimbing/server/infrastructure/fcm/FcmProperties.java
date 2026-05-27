package com.holaclimbing.server.infrastructure.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM 설정. enabled=false 또는 자격증명 미설정 시 NoopFcmSender가 사용된다.
 * credentialsPath는 Firebase 서비스 계정 키(JSON) 파일 경로. 비어 있으면
 * GOOGLE_APPLICATION_CREDENTIALS 환경변수의 자격증명을 자동 사용한다.
 */
@ConfigurationProperties(prefix = "app.fcm")
public record FcmProperties(
        boolean enabled,
        String credentialsPath
) {
}
