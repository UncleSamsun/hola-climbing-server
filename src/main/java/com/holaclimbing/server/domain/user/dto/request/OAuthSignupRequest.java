package com.holaclimbing.server.domain.user.dto.request;

import com.holaclimbing.server.domain.terms.dto.request.TermAgreementRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OAuthSignupRequest(
        @NotBlank String signupToken,
        @NotBlank @Size(min = 2, max = 20) String nickname,
        @Valid List<TermAgreementRequest> termsAgreed
) {
}
