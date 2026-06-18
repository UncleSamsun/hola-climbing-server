package com.holaclimbing.server.domain.terms;

import static com.holaclimbing.server.common.exception.error.ErrorCode.*;

import com.holaclimbing.server.common.exception.docs.ApiErrorCodes;
import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.terms.dto.request.AgreeTermsRequest;
import com.holaclimbing.server.domain.terms.dto.response.TermResponse;
import com.holaclimbing.server.domain.terms.service.TermsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 약관 API. 활성 약관 조회는 공개, 동의 기록은 인증이 필요하다.
 */
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @ApiErrorCodes({TERMS_NOT_CONFIGURED})
    @GetMapping
    public ApiResponse<List<TermResponse>> getActiveTerms() {
        return ApiResponse.success(termsService.getActiveTerms());
    }

    @ApiErrorCodes({INVALID_INPUT, TERMS_NOT_CONFIGURED})
    @PostMapping("/agree")
    public ApiResponse<Void> agree(@AuthenticationPrincipal Long userId,
                                   @Valid @RequestBody AgreeTermsRequest request) {
        termsService.agree(userId, request.agreements());
        return ApiResponse.success();
    }
}
