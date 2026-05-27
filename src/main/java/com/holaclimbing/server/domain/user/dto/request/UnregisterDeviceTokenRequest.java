package com.holaclimbing.server.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * FCM 디바이스 토큰 해제 요청. 본인 소유 토큰만 삭제된다.
 */
public record UnregisterDeviceTokenRequest(
        @NotBlank @Size(max = 500) String token
) {
}
