package com.holaclimbing.server.domain.stats.mapper;

import com.holaclimbing.server.domain.stats.domain.Stats;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatsMapper {

    /** 사용자 통계 단건 조회. 분석 데이터가 없으면 null. */
    Stats findByUserId(Long userId);
}
