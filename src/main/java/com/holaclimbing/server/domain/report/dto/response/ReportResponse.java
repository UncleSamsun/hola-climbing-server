package com.holaclimbing.server.domain.report.dto.response;

import com.holaclimbing.server.domain.report.domain.Report;

public record ReportResponse(Long reportId) {

    public static ReportResponse of(Report report) {
        return new ReportResponse(report.getId());
    }
}
