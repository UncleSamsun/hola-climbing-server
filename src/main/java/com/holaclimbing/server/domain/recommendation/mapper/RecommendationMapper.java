package com.holaclimbing.server.domain.recommendation.mapper;

import com.holaclimbing.server.domain.recommendation.dto.RecommendationCursor;
import com.holaclimbing.server.domain.video.domain.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecommendationMapper {

    /** 추천 피드 — 공개 영상 (본인 제외), 벡터 거리 + 팔로잉 boost + 최신순 fallback. */
    List<Video> findFeedVideos(@Param("userId") Long userId,
                               @Param("cursor") RecommendationCursor cursor,
                               @Param("limit") int limit);
}
