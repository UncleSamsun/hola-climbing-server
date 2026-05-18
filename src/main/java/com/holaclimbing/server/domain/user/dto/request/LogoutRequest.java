package com.holaclimbing.server.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 요청. Access 토큰은 Authorization 헤더로, Refresh 토큰은 본문으로 받는다.
 */
public record LogoutRequest(
        @NotBlank String refreshToken
) {
}
