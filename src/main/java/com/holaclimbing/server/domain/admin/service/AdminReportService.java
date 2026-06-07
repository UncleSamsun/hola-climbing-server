package com.holaclimbing.server.domain.admin.service;

import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.admin.dto.request.AdminReportStatusRequest;
import com.holaclimbing.server.domain.admin.dto.response.AdminReportResponse;

public interface AdminReportService {

    PageResponse<AdminReportResponse> search(String status, String targetType, String category, int page, int size);

    AdminReportResponse changeStatus(Long adminId, Long reportId, AdminReportStatusRequest request);
}
