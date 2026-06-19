package com.holaclimbing.server.domain.recommendation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecommendationInteractionMapper {

    int upsertImpressions(@Param("userId") Long userId,
                          @Param("videoIds") List<Long> videoIds);

    int upsertView(@Param("userId") Long userId,
                   @Param("videoId") Long videoId);
}
