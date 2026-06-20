package com.holaclimbing.server.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthResultRequest(
        @NotBlank String code
) {
}
