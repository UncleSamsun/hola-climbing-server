package com.holaclimbing.server.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * FCM 디바이스 토큰 등록 요청. 같은 token이 이미 있으면 user_id를 현재 사용자로 갱신한다.
 */
public record RegisterDeviceTokenRequest(
        @NotBlank @Size(max = 500) String token,
        @NotBlank @Pattern(regexp = "ios|android|web") String platform
) {
}
