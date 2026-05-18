package com.holaclimbing.server.domain.stats.dto.response;

import com.holaclimbing.server.domain.stats.domain.Stats;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 사용자 클라이밍 통계 응답.
 * techniqueCounts: 동작별 누적 횟수 (예: {"highstep": 12, "flagging": 8}).
 */
public record UserStatsResponse(
        Long userId,
        int totalVideos,
        long totalClimbingSeconds,
        Map<String, Integer> techniqueCounts,
        LocalDateTime lastClimbedAt
) {
    public static UserStatsResponse of(Stats stats, Map<String, Integer> techniqueCounts) {
        return new UserStatsResponse(stats.getUserId(), stats.getTotalVideos(),
                stats.getTotalClimbingSeconds(), techniqueCounts, stats.getLastClimbedAt());
    }

    /** 아직 분석 데이터가 없는 사용자 — 0으로 채운 통계. */
    public static UserStatsResponse empty(Long userId) {
        return new UserStatsResponse(userId, 0, 0L, Map.of(), null);
    }
}
