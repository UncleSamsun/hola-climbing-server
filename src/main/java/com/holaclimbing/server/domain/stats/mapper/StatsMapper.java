package com.holaclimbing.server.domain.stats.mapper;

import com.holaclimbing.server.domain.stats.domain.DynamicSegmentCounts;
import com.holaclimbing.server.domain.stats.domain.Stats;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatsMapper {

    /** 사용자 영상과 대표 분석 결과에서 조회 시점 기준 통계를 집계한다. */
    Stats findByUserId(Long userId);

    /**
     * 해당 사용자가 올린 모든 영상(soft-delete 제외)의 대표 분석 결과에서
     * final_is_dynamic = true / false 개수를 한 번에 집계. 분석 데이터가 없으면 둘 다 0.
     */
    DynamicSegmentCounts findDynamicSegmentCountsByUserId(Long userId);
}
