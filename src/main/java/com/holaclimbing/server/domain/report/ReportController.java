package com.holaclimbing.server.domain.report;

import static com.holaclimbing.server.common.exception.error.ErrorCode.*;

import com.holaclimbing.server.common.exception.docs.ApiErrorCodes;
import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.report.dto.request.CreateReportRequest;
import com.holaclimbing.server.domain.report.dto.response.ReportResponse;
import com.holaclimbing.server.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신고 API. 등록은 인증이 필요하다.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @ApiErrorCodes({INVALID_INPUT, SELF_REPORT_NOT_ALLOWED, ALREADY_REPORTED, USER_NOT_FOUND, VIDEO_NOT_FOUND, NOT_FOUND})
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reportService.createReport(userId, request)));
    }
}
