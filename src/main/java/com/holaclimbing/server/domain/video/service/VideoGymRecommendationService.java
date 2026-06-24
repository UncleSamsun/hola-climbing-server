package com.holaclimbing.server.domain.video.service;

import com.holaclimbing.server.domain.video.dto.response.VideoRecommendedGymResponse;

import java.util.List;

public interface VideoGymRecommendationService {

    List<VideoRecommendedGymResponse> getRecommendedGyms(Long videoId,
                                                         Long viewerId,
                                                         double lat,
                                                         double lng,
                                                         double radiusKm,
                                                         int size);
}
