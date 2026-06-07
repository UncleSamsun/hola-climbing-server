package com.holaclimbing.server.domain.admin.dto.request;

import jakarta.validation.constraints.Size;

public record AdminReasonRequest(
        @Size(max = 500) String reason
) {
}
