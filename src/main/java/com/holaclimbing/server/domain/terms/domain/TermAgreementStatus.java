package com.holaclimbing.server.domain.terms.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 활성 약관별 사용자 동의 상태 조회 모델.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermAgreementStatus {

    private Long termId;
    private String type;
    private String version;
    private String title;
    private Boolean required;
    private Boolean agreed;
    private OffsetDateTime agreedAt;
}
