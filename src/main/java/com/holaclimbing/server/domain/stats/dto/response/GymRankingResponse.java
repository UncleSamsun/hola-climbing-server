package com.holaclimbing.server.domain.stats.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymRankingResponse(
        String period,
        String scope,
        String sort,
        List<Item> content,
        String nextCursor,
        boolean hasNext
) {
    public record Item(
            int rank,
            Long gymId,
            String gymName,
            int visitCount,
            LocalDate latestVisitDate
    ) {
    }
}
