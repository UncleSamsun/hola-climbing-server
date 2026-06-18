package com.holaclimbing.server.domain.analysis.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 영상 단위 분석 결과 피드백 요청.
 * 사용자가 확인한 7가지 동작 포함 여부와 영상의 dynamic/static 여부를 최종값으로 반영한다.
 * 이전 클라이언트의 단일 기술 맞음/틀림 payload도 호환한다.
 */
public record AnalysisFeedbackRequest(
        List<@NotBlank String> techniques,
        @JsonAlias("is_dynamic")
        Boolean isDynamic,
        String note,
        @JsonAlias("technique_label")
        String techniqueLabel,
        @JsonAlias("timestamp_sec")
        Double timestampSec,
        @JsonAlias("is_correct")
        Boolean isCorrect,
        @JsonAlias("correct_label")
        String correctLabel
) {
    @AssertTrue(message = "영상 단위 피드백(techniques,isDynamic) 또는 단일 기술 피드백(techniqueLabel,isCorrect)이 필요합니다.")
    public boolean isValidFeedbackShape() {
        return hasVideoLevelFeedback() || hasLegacyTechniqueFeedback();
    }

    public boolean hasVideoLevelFeedback() {
        return techniques != null && isDynamic != null;
    }

    public boolean hasLegacyTechniqueFeedback() {
        return techniqueLabel != null && !techniqueLabel.isBlank() && isCorrect != null;
    }
}
