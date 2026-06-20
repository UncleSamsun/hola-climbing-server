package com.holaclimbing.server.domain.user;

import static com.holaclimbing.server.common.exception.error.ErrorCode.INVALID_OAUTH_RESULT_CODE;
import static com.holaclimbing.server.common.exception.error.ErrorCode.INVALID_OAUTH_SIGNUP_TOKEN;
import static com.holaclimbing.server.common.exception.error.ErrorCode.INVALID_OAUTH_STATE;
import static com.holaclimbing.server.common.exception.error.ErrorCode.NICKNAME_ALREADY_EXISTS;
import static com.holaclimbing.server.common.exception.error.ErrorCode.OAUTH_AUTHORIZATION_FAILED;
import static com.holaclimbing.server.common.exception.error.ErrorCode.OAUTH_EMAIL_ALREADY_EXISTS;
import static com.holaclimbing.server.common.exception.error.ErrorCode.REQUIRED_TERMS_NOT_AGREED;
import static com.holaclimbing.server.common.exception.error.ErrorCode.TERMS_NOT_CONFIGURED;
import static com.holaclimbing.server.common.exception.error.ErrorCode.UNSUPPORTED_OAUTH_PROVIDER;
import static com.holaclimbing.server.common.exception.error.ErrorCode.USER_SUSPENDED;

import com.holaclimbing.server.common.exception.docs.ApiErrorCodes;
import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.user.dto.request.OAuthResultRequest;
import com.holaclimbing.server.domain.user.dto.request.OAuthSignupRequest;
import com.holaclimbing.server.domain.user.dto.response.OAuthLoginResponse;
import com.holaclimbing.server.domain.user.dto.response.TokenResponse;
import com.holaclimbing.server.domain.user.oauth.OAuthRedirectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
@Validated
public class OAuthRedirectController {

    private final OAuthRedirectService oauthRedirectService;

    @ApiErrorCodes({UNSUPPORTED_OAUTH_PROVIDER, OAUTH_AUTHORIZATION_FAILED})
    @GetMapping("/{provider}/authorize")
    public ResponseEntity<Void> authorize(@PathVariable String provider,
                                          @RequestParam @NotBlank String redirectUri) {
        return redirect(oauthRedirectService.buildAuthorizationRedirect(provider, redirectUri));
    }

    @ApiErrorCodes({UNSUPPORTED_OAUTH_PROVIDER, INVALID_OAUTH_STATE, OAUTH_AUTHORIZATION_FAILED, USER_SUSPENDED})
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider,
                                         @RequestParam(required = false) String code,
                                         @RequestParam String state,
                                         @RequestParam(required = false, name = "error") String error) {
        return redirect(oauthRedirectService.handleCallback(provider, code, state, error));
    }

    @ApiErrorCodes({INVALID_OAUTH_RESULT_CODE})
    @PostMapping("/result")
    public ApiResponse<OAuthLoginResponse> result(@Valid @RequestBody OAuthResultRequest request) {
        return ApiResponse.success(oauthRedirectService.consumeResult(request.code()));
    }

    @ApiErrorCodes({
            INVALID_OAUTH_SIGNUP_TOKEN,
            OAUTH_EMAIL_ALREADY_EXISTS,
            NICKNAME_ALREADY_EXISTS,
            REQUIRED_TERMS_NOT_AGREED,
            TERMS_NOT_CONFIGURED,
            USER_SUSPENDED
    })
    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signup(@Valid @RequestBody OAuthSignupRequest request) {
        return ApiResponse.success(oauthRedirectService.signup(request));
    }

    private ResponseEntity<Void> redirect(String location) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(location));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
