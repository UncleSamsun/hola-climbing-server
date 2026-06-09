package com.holaclimbing.server.domain.recommendation.service;

import com.holaclimbing.server.common.response.CursorPageResponse;
import com.holaclimbing.server.domain.recommendation.dto.response.RecommendedVideoResponse;

public interface RecommendationService {

    /** 홈 피드 추천 — 팔로잉 영상 + 추천 영상 혼합. */
    CursorPageResponse<RecommendedVideoResponse> getVideoFeed(Long userId, String cursor, int size);
}
