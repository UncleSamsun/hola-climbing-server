package com.holaclimbing.server.infrastructure.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * GCS v4 Signed URL 발급.
 * 영상 바이너리는 Spring을 거치지 않고 클라이언트가 GCS와 직접 주고받는다.
 */
@Service
@RequiredArgsConstructor
public class GcsStorageService {

    private final Storage storage;
    private final GcsProperties properties;

    /**
     * 클라이언트가 PUT으로 직접 업로드할 Signed URL.
     * 업로드 PUT 요청은 여기서 서명에 포함된 contentType과 동일한 Content-Type 헤더를 보내야 한다.
     */
    public String createUploadUrl(String objectPath, String contentType) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(properties.bucket(), objectPath))
                    .setContentType(contentType)
                    .build();
            URL url = storage.signUrl(blobInfo, properties.signedUrlMinutes(), TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                    Storage.SignUrlOption.withContentType(),
                    Storage.SignUrlOption.withV4Signature());
            return url.toString();
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.GCS_UPLOAD_FAILED);
        }
    }

    /** 영상 재생용 읽기 Signed URL. objectPath가 없으면 null. */
    public String createReadUrl(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(properties.bucket(), objectPath)).build();
            URL url = storage.signUrl(blobInfo, properties.signedUrlMinutes(), TimeUnit.MINUTES,
                    Storage.SignUrlOption.withV4Signature());
            return url.toString();
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.GCS_UPLOAD_FAILED);
        }
    }
}
