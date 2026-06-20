package com.holaclimbing.server.domain.terms.dto.response;

import java.util.List;

public record TermsAgreementStatusResponse(
        boolean allRequiredAgreed,
        List<TermAgreementStatusItemResponse> terms
) {
}
