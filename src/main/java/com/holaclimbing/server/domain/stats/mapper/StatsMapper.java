package com.holaclimbing.server.domain.stats.mapper;

import com.holaclimbing.server.domain.stats.domain.DynamicSegmentCounts;
import com.holaclimbing.server.domain.stats.domain.Stats;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatsMapper {

    /** 사용자 통계 단건 조회. 분석 데이터가 없으면 null. */
    Stats findByUserId(Long userId);

    /**
     * 해당 사용자가 올린 모든 영상(soft-delete 제외)의 분석 세그먼트에서
     * is_dynamic = true / false 개수를 한 번에 집계. 분석 데이터가 없으면 둘 다 0.
     */
    DynamicSegmentCounts findDynamicSegmentCountsByUserId(Long userId);
}
