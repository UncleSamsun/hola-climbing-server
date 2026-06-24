package com.holaclimbing.server.domain.stats.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GymRankingRow {

    private Long gymId;
    private String gymName;
    private int visitCount;
    private LocalDate latestVisitDate;
}
