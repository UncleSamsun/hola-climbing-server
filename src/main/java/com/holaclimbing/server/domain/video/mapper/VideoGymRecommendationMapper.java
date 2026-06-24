package com.holaclimbing.server.domain.video.mapper;

import com.holaclimbing.server.domain.video.domain.VideoRecommendationSeed;
import com.holaclimbing.server.domain.video.domain.VideoRecommendedGym;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoGymRecommendationMapper {

    VideoRecommendationSeed findSeedVideo(Long videoId);

    List<VideoRecommendedGym> findStyleMatchedGyms(@Param("userId") Long userId,
                                                   @Param("lat") double lat,
                                                   @Param("lng") double lng,
                                                   @Param("radiusKm") double radiusKm,
                                                   @Param("limit") int limit,
                                                   @Param("techniques") List<String> techniques,
                                                   @Param("seedIsDynamic") Boolean seedIsDynamic,
                                                   @Param("minAnalyzedVideos") int minAnalyzedVideos);

    List<VideoRecommendedGym> findNearbyGyms(@Param("userId") Long userId,
                                             @Param("lat") double lat,
                                             @Param("lng") double lng,
                                             @Param("radiusKm") double radiusKm,
                                             @Param("limit") int limit);
}
