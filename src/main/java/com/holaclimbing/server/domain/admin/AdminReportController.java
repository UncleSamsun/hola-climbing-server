package com.holaclimbing.server.domain.admin;

import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.admin.dto.request.AdminReportStatusRequest;
import com.holaclimbing.server.domain.admin.dto.response.AdminReportResponse;
import com.holaclimbing.server.domain.admin.service.AdminReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Validated
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public ApiResponse<PageResponse<AdminReportResponse>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size) {
        return ApiResponse.success(adminReportService.search(status, targetType, category, page, size));
    }

    @PatchMapping("/{reportId}/status")
    public ApiResponse<AdminReportResponse> changeStatus(@AuthenticationPrincipal Long adminId,
                                                         @PathVariable Long reportId,
                                                         @Valid @RequestBody AdminReportStatusRequest request) {
        return ApiResponse.success(adminReportService.changeStatus(adminId, reportId, request));
    }
}
