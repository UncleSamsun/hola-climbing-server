package com.holaclimbing.server.infrastructure.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.holaclimbing.server.domain.user.mapper.DeviceTokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * FCM 빈 구성.
 * <ul>
 *   <li>app.fcm.enabled=true → FirebaseApp 초기화 + FirebaseFcmSender 등록</li>
 *   <li>그 외 → NoopFcmSender 등록 (개발·테스트 환경 기본값)</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FcmProperties.class)
public class FcmConfig {

    @Bean
    @ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FcmProperties properties) throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        GoogleCredentials credentials;
        if (properties.credentialsPath() != null && !properties.credentialsPath().isBlank()) {
            try (InputStream in = new FileInputStream(properties.credentialsPath())) {
                credentials = GoogleCredentials.fromStream(in);
            }
        } else {
            // GOOGLE_APPLICATION_CREDENTIALS 환경변수에서 자동 로드
            credentials = GoogleCredentials.getApplicationDefault();
        }
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        log.info("[FCM] FirebaseApp 초기화 완료");
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    @ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
    public FcmSender firebaseFcmSender(FirebaseApp firebaseApp, DeviceTokenMapper deviceTokenMapper) {
        return new FirebaseFcmSender(deviceTokenMapper);
    }

    @Bean
    @ConditionalOnMissingBean(FcmSender.class)
    public FcmSender noopFcmSender() {
        return new NoopFcmSender();
    }
}
