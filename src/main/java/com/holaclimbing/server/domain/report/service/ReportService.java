package com.holaclimbing.server.domain.report.service;

import com.holaclimbing.server.domain.report.dto.request.CreateReportRequest;
import com.holaclimbing.server.domain.report.dto.response.ReportResponse;

public interface ReportService {

    /** 신고 등록. 같은 신고자가 같은 대상을 중복 신고할 수 없다. */
    ReportResponse createReport(Long reporterId, CreateReportRequest request);
}
