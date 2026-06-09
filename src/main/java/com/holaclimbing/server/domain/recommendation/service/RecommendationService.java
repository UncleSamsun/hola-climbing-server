package com.holaclimbing.server.domain.recommendation.service;

import com.holaclimbing.server.common.response.CursorPageResponse;
import com.holaclimbing.server.domain.recommendation.dto.response.RecommendedGymResponse;
import com.holaclimbing.server.domain.recommendation.dto.response.RecommendedVideoResponse;

import java.util.List;

public interface RecommendationService {

    /** 홈 피드 추천 — 팔로잉 영상 + 추천 영상 혼합. */
    CursorPageResponse<RecommendedVideoResponse> getVideoFeed(Long userId, String cursor, int size);

    /** 주변 암장 추천 — 반경 필터 + 스타일 유사도 우선 + 거리 fallback. */
    List<RecommendedGymResponse> getNearbyGyms(Long userId, double lat, double lng, double radiusKm, int size);
}
