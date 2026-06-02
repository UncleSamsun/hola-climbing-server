package com.holaclimbing.server.domain.stats.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 올린 모든 영상의 분석 세그먼트 중 dynamic / static 동작 개수.
 * is_dynamic 컬럼 기준 집계 결과.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DynamicSegmentCounts {

    private long dynamicCount;
    private long staticCount;

    /** dynamic 세그먼트 수가 static보다 많으면 true (동률·없음은 false). */
    public boolean isDynamic() {
        return dynamicCount > staticCount;
    }

    public static DynamicSegmentCounts empty() {
        return new DynamicSegmentCounts(0L, 0L);
    }
}
