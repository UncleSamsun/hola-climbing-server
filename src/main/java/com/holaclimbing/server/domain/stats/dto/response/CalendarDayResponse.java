package com.holaclimbing.server.domain.stats.dto.response;

import java.time.LocalDate;

/**
 * 달력의 하루치 요약. 해당 날짜에 작성한 기록 수와 푼 문제 총합.
 */
public record CalendarDayResponse(
        LocalDate date,
        int logCount,
        int totalProblems
) {
}
