package com.holaclimbing.server.domain.user;

import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.user.dto.request.RegisterDeviceTokenRequest;
import com.holaclimbing.server.domain.user.dto.request.UnregisterDeviceTokenRequest;
import com.holaclimbing.server.domain.user.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FCM 디바이스 토큰 등록·해제 API. 모두 인증 필요.
 */
@RestController
@RequestMapping("/api/users/me/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        deviceTokenService.register(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @DeleteMapping
    public ApiResponse<Void> unregisterToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UnregisterDeviceTokenRequest request) {
        deviceTokenService.unregister(userId, request);
        return ApiResponse.success();
    }
}
