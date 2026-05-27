package com.holaclimbing.server.infrastructure.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.holaclimbing.server.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GCS Signed URL + Storage SDK I/O 통합 테스트.
 * fake-gcs-server를 띄워 두 가지를 검증한다:
 * <ol>
 *   <li>{@link GcsStorageService}가 v4 Signed URL을 정상 발급한다 (호스트·경로·서명 파라미터 포함)</li>
 *   <li>Storage SDK ↔ fake-gcs-server 가 실제로 객체를 업로드/다운로드한다</li>
 * </ol>
 *
 * <p>주의: fake-gcs-server는 v4 Signed URL의 서명을 검증하지 않고,
 * XML API 라우팅이 실제 GCS와 일부 다르다.
 * "발급된 URL로 PUT 해서 GCS가 받아준다"의 운영 충실도는 실제 dev 버킷 수동 smoke test로 보강해야 한다.</p>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class GcsSignedUrlIntegrationTest {

    private static final String CONTENT_TYPE = "video/mp4";

    @Autowired
    private GcsStorageService gcsStorageService;

    @Autowired
    private Storage storage;

    @Autowired
    private GcsProperties props;

    @Test
    @DisplayName("Signed URL 발급 — 업로드 URL이 객체 경로·서명·요구 헤더 파라미터를 포함한다")
    void createUploadUrl_includesExpectedParts() {
        String objectPath = "videos/uploads/test-" + System.nanoTime() + ".mp4";

        String uploadUrl = gcsStorageService.createUploadUrl(objectPath, CONTENT_TYPE);

        assertThat(uploadUrl)
                .contains(objectPath)
                .contains("X-Goog-Algorithm=GOOG4-RSA-SHA256")
                .contains("X-Goog-Signature=")
                .contains("X-Goog-Expires=");
    }

    @Test
    @DisplayName("Signed URL 발급 — 읽기 URL이 객체 경로·서명을 포함한다")
    void createReadUrl_includesExpectedParts() {
        String objectPath = "videos/uploads/test-" + System.nanoTime() + ".mp4";

        String readUrl = gcsStorageService.createReadUrl(objectPath);

        assertThat(readUrl)
                .contains(objectPath)
                .contains("X-Goog-Signature=");
    }

    @Test
    @DisplayName("createReadUrl — objectPath가 비어 있으면 null 반환 (gcs_path 미저장 영상 대응)")
    void createReadUrl_nullOrBlank_returnsNull() {
        assertThat(gcsStorageService.createReadUrl(null)).isNull();
        assertThat(gcsStorageService.createReadUrl("")).isNull();
    }

    @Test
    @DisplayName("Storage SDK I/O — 업로드한 바이트를 다시 다운로드하면 동일하다")
    void storageSdk_uploadAndDownload_roundTrip() {
        String objectPath = "videos/uploads/sdk-" + System.nanoTime() + ".mp4";
        byte[] payload = "fake-video-binary-payload".getBytes(StandardCharsets.UTF_8);

        storage.create(
                BlobInfo.newBuilder(BlobId.of(props.bucket(), objectPath))
                        .setContentType(CONTENT_TYPE)
                        .build(),
                payload);

        byte[] downloaded = storage.readAllBytes(BlobId.of(props.bucket(), objectPath));
        assertThat(downloaded).isEqualTo(payload);
    }
}
