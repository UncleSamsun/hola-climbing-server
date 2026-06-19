package com.holaclimbing.server.infrastructure.gcs;

import com.google.auth.ServiceAccountSigner;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
                new GcsProperties(
                        "hola-climbing-log-videos",
                        "videos/uploads",
                        15,
                        "hola-climbing-thumbnails-public",
                        "https://storage.googleapis.com/hola-climbing-thumbnails-public/"),
                signer());

        String uploadUrl = service.createUploadUrl("videos/uploads/1/clip.mp4", "video/mp4");

        assertThat(URI.create(uploadUrl).getRawPath())
                .isEqualTo("/hola-climbing-log-videos/videos/uploads/1/clip.mp4");
    }

    @Test
    @DisplayName("createPublicThumbnailUrl — public base URL과 objectPath를 조합하고 Signed URL을 만들지 않는다")
    void createPublicThumbnailUrl_joinsPublicBaseUrlAndObjectPath() throws Exception {
        Storage storage = StorageOptions.newBuilder()
                .setHost("https://storage.googleapis.com/")
                .setProjectId("hola-test")
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
        GcsStorageService service = new GcsStorageService(
                storage,
                new GcsProperties(
                        "hola-climbing-log-videos",
                        "videos/uploads",
                        15,
                        "hola-climbing-thumbnails-public",
                        "https://storage.googleapis.com/hola-climbing-thumbnails-public/"),
                signer());

        String thumbnailUrl = service.createPublicThumbnailUrl("/videos/thumbnails/1/thumb.jpg");

        assertThat(thumbnailUrl)
                .isEqualTo("https://storage.googleapis.com/hola-climbing-thumbnails-public/videos/thumbnails/1/thumb.jpg")
                .doesNotContain("X-Goog-Signature=");
    }

    @Test
    @DisplayName("uploadPublicThumbnailBytes — public thumbnail bucket에 객체를 저장한다")
    void uploadPublicThumbnailBytes_usesPublicThumbnailBucket() throws Exception {
        Storage storage = mock(Storage.class);
        GcsStorageService service = new GcsStorageService(
                storage,
                new GcsProperties(
                        "hola-climbing-log-videos",
                        "videos/uploads",
                        15,
                        "hola-climbing-thumbnails-public",
                        "https://storage.googleapis.com/hola-climbing-thumbnails-public/"),
                signer());

        service.uploadPublicThumbnailBytes("videos/thumbnails/1/thumb.jpg", "image/jpeg", "jpeg".getBytes());

        verify(storage).create(eq(BlobInfo.newBuilder(
                "hola-climbing-thumbnails-public",
                "videos/thumbnails/1/thumb.jpg")
                .setContentType("image/jpeg")
                .build()), aryEq("jpeg".getBytes()));
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
