package com.holaclimbing.server.domain.admin.dto.response;

public record AdminDashboardResponse(
        long pendingGymCount,
        long pendingReportCount,
        long failedAnalysisVideoCount,
        long newUserCountToday
) {
}
