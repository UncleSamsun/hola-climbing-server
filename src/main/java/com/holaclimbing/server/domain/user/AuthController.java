package com.holaclimbing.server.domain.user;

import static com.holaclimbing.server.common.exception.error.ErrorCode.*;

import com.holaclimbing.server.common.exception.docs.ApiErrorCodes;
import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.user.dto.request.LoginRequest;
import com.holaclimbing.server.domain.user.dto.request.LogoutRequest;
import com.holaclimbing.server.domain.user.dto.request.PasswordResetEmailRequest;
import com.holaclimbing.server.domain.user.dto.request.PasswordResetRequest;
import com.holaclimbing.server.domain.user.dto.request.RefreshRequest;
import com.holaclimbing.server.domain.user.dto.request.ResendVerificationRequest;
import com.holaclimbing.server.domain.user.dto.request.SignupRequest;
import com.holaclimbing.server.domain.user.dto.request.VerifyEmailRequest;
import com.holaclimbing.server.domain.user.dto.response.AvailabilityResponse;
import com.holaclimbing.server.domain.user.dto.response.SignupResponse;
import com.holaclimbing.server.domain.user.dto.response.TokenResponse;
import com.holaclimbing.server.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API — 회원가입, 로그인, 토큰 재발급, 이메일 인증.
 * 회원 프로필·팔로우·차단은 UserProfileController(/api/users)에 있다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;

    @ApiErrorCodes({EMAIL_ALREADY_EXISTS, NICKNAME_ALREADY_EXISTS})
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "인증 메일을 발송했습니다. 메일 확인 후 로그인해 주세요."));
    }

    @ApiErrorCodes({USER_NOT_FOUND, PASSWORD_MISMATCH, EMAIL_NOT_VERIFIED, USER_SUSPENDED})
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @ApiErrorCodes({INVALID_TOKEN, EXPIRED_TOKEN, USER_NOT_FOUND, USER_SUSPENDED})
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(userService.refresh(request.refreshToken()));
    }

    @ApiErrorCodes({INVALID_TOKEN})
    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request.token());
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody LogoutRequest request) {
        userService.logout(stripBearer(authHeader), request.refreshToken());
        return ApiResponse.success();
    }

    @PostMapping("/password/reset-request")
    public ApiResponse<Void> requestPasswordReset(@Valid @RequestBody PasswordResetEmailRequest request) {
        userService.requestPasswordReset(request.email());
        return ApiResponse.success();
    }

    @ApiErrorCodes({INVALID_RESET_TOKEN})
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.success();
    }

    @ApiErrorCodes({USER_NOT_FOUND, INVALID_INPUT})
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        userService.resendVerification(request.email());
        return ApiResponse.success();
    }

    @GetMapping("/email-check")
    public ApiResponse<AvailabilityResponse> checkEmail(@RequestParam @NotBlank @Email String email) {
        return ApiResponse.success(new AvailabilityResponse(userService.isEmailAvailable(email)));
    }

    @GetMapping("/nickname-check")
    public ApiResponse<AvailabilityResponse> checkNickname(@RequestParam @NotBlank String nickname) {
        return ApiResponse.success(new AvailabilityResponse(userService.isNicknameAvailable(nickname)));
    }

    private static String stripBearer(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length());
        }
        return authHeader;
    }
}
