package com.holaclimbing.server;

import com.holaclimbing.server.domain.terms.dto.request.TermAgreementRequest;
import com.holaclimbing.server.domain.user.dto.request.SignupRequest;

import java.util.List;

public final class TestSignupRequests {

    private static final long TERM_SERVICE = 1L;
    private static final long TERM_PRIVACY = 2L;
    private static final long TERM_MARKETING = 3L;
    private static final long TERM_LOCATION = 4L;

    private TestSignupRequests() {
    }

    public static SignupRequest signupRequest(String email, String password, String nickname) {
        return signupRequest(email, password, nickname, List.of(
                new TermAgreementRequest(TERM_SERVICE, true),
                new TermAgreementRequest(TERM_PRIVACY, true),
                new TermAgreementRequest(TERM_MARKETING, false),
                new TermAgreementRequest(TERM_LOCATION, false)));
    }

    public static SignupRequest signupRequest(
            String email,
            String password,
            String nickname,
            List<TermAgreementRequest> termsAgreed
    ) {
        return new SignupRequest(email, password, nickname, termsAgreed);
    }
}
