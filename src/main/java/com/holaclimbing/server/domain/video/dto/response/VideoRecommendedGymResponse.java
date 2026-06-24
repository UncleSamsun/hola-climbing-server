package com.holaclimbing.server.domain.video.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.holaclimbing.server.domain.gym.dto.DayHours;
import com.holaclimbing.server.domain.video.domain.VideoRecommendedGym;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoRecommendedGymResponse(
        Long id,
        String name,
        String address,
        String thumbnailUrl,
        String regionCode,
        BigDecimal ratingAvg,
        int ratingCount,
        Map<String, DayHours> businessHours,
        boolean isOpen,
        boolean isFavorite,
        Double distanceKm,
        Double similarityScore,
        Double techniqueScore,
        Double dynamicScore,
        Double locationRatingScore,
        String source,
        List<String> reasons
) {
    public static VideoRecommendedGymResponse from(VideoRecommendedGym gym,
                                                   String thumbnailUrl,
                                                   Map<String, DayHours> businessHours,
                                                   boolean isOpen,
                                                   boolean isFavorite,
                                                   String source,
                                                   List<String> reasons) {
        return new VideoRecommendedGymResponse(
                gym.getId(),
                gym.getName(),
                gym.getAddress(),
                thumbnailUrl,
                gym.getRegionCode(),
                gym.getRatingAvg(),
                gym.getRatingCount(),
                businessHours,
                isOpen,
                isFavorite,
                gym.getDistanceKm(),
                gym.getSimilarityScore(),
                gym.getTechniqueScore(),
                gym.getDynamicScore(),
                gym.getLocationRatingScore(),
                source,
                reasons == null ? List.of() : List.copyOf(reasons));
    }
}
