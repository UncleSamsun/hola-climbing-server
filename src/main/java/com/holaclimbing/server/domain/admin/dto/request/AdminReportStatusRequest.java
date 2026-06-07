package com.holaclimbing.server.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReportStatusRequest(
        @NotBlank String status,
        @NotBlank String resolutionAction,
        @Size(max = 500) String reason
) {
}
