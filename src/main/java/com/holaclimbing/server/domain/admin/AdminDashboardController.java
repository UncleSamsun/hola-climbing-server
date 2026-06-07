package com.holaclimbing.server.domain.admin;

import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.admin.dto.response.AdminDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    @GetMapping
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(new AdminDashboardResponse(0, 0, 0, 0));
    }
}
