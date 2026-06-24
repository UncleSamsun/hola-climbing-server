package com.holaclimbing.server.domain.stats.dto;

import java.time.LocalDate;

public record GymRankingCursor(
        int visitCount,
        LocalDate latestVisitDate,
        Long gymId,
        int rank
) {
}
