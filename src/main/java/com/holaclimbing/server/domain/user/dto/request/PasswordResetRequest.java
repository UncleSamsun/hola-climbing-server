package com.holaclimbing.server.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 요청. 메일로 받은 토큰과 새 비밀번호를 함께 보낸다.
 */
public record PasswordResetRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 64) String newPassword
) {
}
