package com.holaclimbing.server.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserStatusRequest(
        @NotBlank String status,
        @Size(max = 500) String reason
) {
}
