package com.holaclimbing.server.domain.terms.dto.response;

import com.holaclimbing.server.domain.terms.domain.TermAgreementStatus;

import java.time.OffsetDateTime;

public record TermAgreementStatusItemResponse(
        Long termId,
        String type,
        String version,
        String title,
        boolean required,
        boolean agreed,
        OffsetDateTime agreedAt
) {
    public static TermAgreementStatusItemResponse of(TermAgreementStatus status) {
        return new TermAgreementStatusItemResponse(
                status.getTermId(),
                status.getType(),
                status.getVersion(),
                status.getTitle(),
                Boolean.TRUE.equals(status.getRequired()),
                Boolean.TRUE.equals(status.getAgreed()),
                status.getAgreedAt());
    }
}
