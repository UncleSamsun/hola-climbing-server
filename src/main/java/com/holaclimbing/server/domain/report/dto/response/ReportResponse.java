package com.holaclimbing.server.domain.report.dto.response;

import com.holaclimbing.server.domain.report.domain.Report;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long reporterId,
        String targetType,
        Long targetId,
        String reasonCode,
        String reasonDetail,
        String status,
        LocalDateTime createdAt
) {
    public static ReportResponse of(Report report) {
        return new ReportResponse(
                report.getId(), report.getReporterId(), report.getTargetType(),
                report.getTargetId(), report.getReasonCode(), report.getReasonDetail(),
                report.getStatus(), report.getCreatedAt());
    }
}
