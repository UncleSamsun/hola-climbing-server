package com.holaclimbing.server.infrastructure.gcs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.holaclimbing.server.domain.video.VideoUploadProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * GCS 연동 설정. Storage 클라이언트는 GOOGLE_APPLICATION_CREDENTIALS 환경변수로 인증한다.
 * test 프로파일에서는 TestcontainersConfiguration이 대체 Storage 빈을 제공한다.
 */
@Configuration
@EnableConfigurationProperties({GcsProperties.class, VideoUploadProperties.class})
public class GcsConfig {

    @Bean
    @Profile("!test")
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}
