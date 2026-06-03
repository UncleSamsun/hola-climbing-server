package com.holaclimbing.server.domain.stats;

import static com.holaclimbing.server.common.exception.error.ErrorCode.*;

import com.holaclimbing.server.common.exception.docs.ApiErrorCodes;
import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.stats.dto.request.CreateClimbingLogRequest;
import com.holaclimbing.server.domain.stats.dto.request.UpdateClimbingLogRequest;
import com.holaclimbing.server.domain.stats.dto.response.ClimbingLogResponse;
import com.holaclimbing.server.domain.stats.service.ClimbingLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클라이밍 기록 CRUD API (F-03-03). 모두 인증이 필요하며 작성자 본인만 접근 가능.
 */
@RestController
@RequestMapping("/api/climbing-logs")
@RequiredArgsConstructor
public class ClimbingLogController {

    private final ClimbingLogService climbingLogService;

    @ApiErrorCodes({GYM_NOT_FOUND})
    @PostMapping
    public ResponseEntity<ApiResponse<ClimbingLogResponse>> createLog(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateClimbingLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(climbingLogService.createLog(userId, request)));
    }

    @ApiErrorCodes({CLIMBING_LOG_NOT_FOUND, FORBIDDEN})
    @GetMapping("/{logId}")
    public ApiResponse<ClimbingLogResponse> getLog(@AuthenticationPrincipal Long userId,
                                                   @PathVariable Long logId) {
        return ApiResponse.success(climbingLogService.getLog(userId, logId));
    }

    @ApiErrorCodes({CLIMBING_LOG_NOT_FOUND, FORBIDDEN, GYM_NOT_FOUND})
    @PatchMapping("/{logId}")
    public ApiResponse<ClimbingLogResponse> updateLog(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long logId,
            @Valid @RequestBody UpdateClimbingLogRequest request) {
        return ApiResponse.success(climbingLogService.updateLog(userId, logId, request));
    }

    @ApiErrorCodes({CLIMBING_LOG_NOT_FOUND, FORBIDDEN})
    @DeleteMapping("/{logId}")
    public ApiResponse<Void> deleteLog(@AuthenticationPrincipal Long userId,
                                       @PathVariable Long logId) {
        climbingLogService.deleteLog(userId, logId);
        return ApiResponse.success();
    }
}
