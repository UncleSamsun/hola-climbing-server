package com.holaclimbing.server.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 회원 탈퇴 요청. 본인 확인용 비밀번호와 선택적 탈퇴 사유.
 */
public record WithdrawRequest(
        @NotBlank String password,
        String reason
) {
}
