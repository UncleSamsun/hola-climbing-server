package com.holaclimbing.server.infrastructure.gcs;

import com.google.auth.ServiceAccountSigner;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class GcsStorageServiceTest {

    @Test
    @DisplayName("운영 GCS 호스트에 trailing slash가 있어도 Signed URL path는 /bucket/object 형식이다")
    void createUploadUrl_defaultGcsHostWithTrailingSlash_doesNotCreateDoubleSlashPath() throws Exception {
        Storage storage = StorageOptions.newBuilder()
                .setHost("https://storage.googleapis.com/")
                .setProjectId("hola-test")
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
        GcsStorageService service = new GcsStorageService(
                storage,
                new GcsProperties("hola-climbing-log-videos", "videos/uploads", 15),
                signer());

        String uploadUrl = service.createUploadUrl("videos/uploads/1/clip.mp4", "video/mp4");

        assertThat(URI.create(uploadUrl).getRawPath())
                .isEqualTo("/hola-climbing-log-videos/videos/uploads/1/clip.mp4");
    }

    private ServiceAccountSigner signer() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return ServiceAccountCredentials.newBuilder()
                .setClientEmail("test@hola-test.iam.gserviceaccount.com")
                .setPrivateKey(pair.getPrivate())
                .setPrivateKeyId("test-key-id")
                .setProjectId("hola-test")
                .build();
    }
}
