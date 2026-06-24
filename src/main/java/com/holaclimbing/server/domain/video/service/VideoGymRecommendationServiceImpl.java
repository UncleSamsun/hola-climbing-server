package com.holaclimbing.server.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import com.holaclimbing.server.domain.gym.dto.DayHours;
import com.holaclimbing.server.domain.gym.service.GymOperatingStatusResolver;
import com.holaclimbing.server.domain.gym.service.GymProfileImageUrlResolver;
import com.holaclimbing.server.domain.video.domain.VideoRecommendationSeed;
import com.holaclimbing.server.domain.video.domain.VideoRecommendedGym;
import com.holaclimbing.server.domain.video.dto.response.VideoRecommendedGymResponse;
import com.holaclimbing.server.domain.video.mapper.VideoGymRecommendationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VideoGymRecommendationServiceImpl implements VideoGymRecommendationService {

    private static final int MIN_ANALYZED_VIDEOS_PER_GYM = 2;
    private static final double MIN_LAT = -90.0;
    private static final double MAX_LAT = 90.0;
    private static final double MIN_LNG = -180.0;
    private static final double MAX_LNG = 180.0;
    private static final String SOURCE_STYLE_MATCH = "video_style_match";
    private static final String SOURCE_NEARBY = "nearby";
    private static final String REASON_TECHNIQUE = "technique";
    private static final String REASON_DYNAMIC = "dynamic";
    private static final String REASON_DISTANCE = "distance";
    private static final String REASON_RATING = "rating";
    private static final BigDecimal RATING_REASON_THRESHOLD = BigDecimal.valueOf(4.30);
    private static final double TECHNIQUE_REASON_THRESHOLD = 0.35;
    private static final double DYNAMIC_REASON_THRESHOLD = 0.10;

    private final VideoGymRecommendationMapper recommendationMapper;
    private final ObjectMapper objectMapper;
    private final GymProfileImageUrlResolver profileImageUrlResolver;
    private final GymOperatingStatusResolver operatingStatusResolver;

    @Override
    @Transactional(readOnly = true)
    public List<VideoRecommendedGymResponse> getRecommendedGyms(Long videoId,
                                                                Long viewerId,
                                                                double lat,
                                                                double lng,
                                                                double radiusKm,
                                                                int size) {
        validateRequest(lat, lng, radiusKm, size);
        VideoRecommendationSeed seed = recommendationMapper.findSeedVideo(videoId);
        if (seed == null) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }
        if (!seed.isPublic()) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_ACCESSIBLE);
        }

        List<String> techniques = parseFinalTechniques(seed.getFinalTechniques());
        if (techniques.isEmpty()) {
            return getNearbyFallback(viewerId, lat, lng, radiusKm, size);
        }

        List<VideoRecommendedGym> matchedGyms = recommendationMapper.findStyleMatchedGyms(
                viewerId, lat, lng, radiusKm, size, techniques, seed.getFinalIsDynamic(), MIN_ANALYZED_VIDEOS_PER_GYM);
        if (matchedGyms.isEmpty()) {
            return getNearbyFallback(viewerId, lat, lng, radiusKm, size);
        }
        return toResponses(matchedGyms, radiusKm, SOURCE_STYLE_MATCH);
    }

    private List<VideoRecommendedGymResponse> getNearbyFallback(Long viewerId,
                                                                double lat,
                                                                double lng,
                                                                double radiusKm,
                                                                int size) {
        return toResponses(recommendationMapper.findNearbyGyms(viewerId, lat, lng, radiusKm, size),
                radiusKm,
                SOURCE_NEARBY);
    }

    private List<VideoRecommendedGymResponse> toResponses(List<VideoRecommendedGym> gyms,
                                                          double radiusKm,
                                                          String source) {
        return gyms.stream()
                .map(gym -> {
                    Map<String, DayHours> businessHours =
                            operatingStatusResolver.parseBusinessHours(gym.getBusinessHours());
                    return VideoRecommendedGymResponse.from(
                            gym,
                            profileImageUrlResolver.resolve(gym.getThumbnailUrl()),
                            businessHours,
                            operatingStatusResolver.isOpenNow(businessHours),
                            Boolean.TRUE.equals(gym.getFavorite()),
                            source,
                            resolveReasons(gym, radiusKm, source));
                })
                .toList();
    }

    private List<String> resolveReasons(VideoRecommendedGym gym, double radiusKm, String source) {
        List<String> reasons = new ArrayList<>();
        if (SOURCE_STYLE_MATCH.equals(source)) {
            if (scoreAtLeast(gym.getTechniqueScore(), TECHNIQUE_REASON_THRESHOLD)) {
                reasons.add(REASON_TECHNIQUE);
            }
            if (scoreAtLeast(gym.getDynamicScore(), DYNAMIC_REASON_THRESHOLD)) {
                reasons.add(REASON_DYNAMIC);
            }
        }
        if (SOURCE_NEARBY.equals(source) || isCloseEnough(gym.getDistanceKm(), radiusKm)) {
            reasons.add(REASON_DISTANCE);
        }
        if (isHighlyRated(gym.getRatingAvg())) {
            reasons.add(REASON_RATING);
        }
        if (reasons.isEmpty()) {
            reasons.add(REASON_DISTANCE);
        }
        return List.copyOf(reasons);
    }

    private boolean scoreAtLeast(Double score, double threshold) {
        return score != null && score >= threshold;
    }

    private boolean isCloseEnough(Double distanceKm, double radiusKm) {
        return distanceKm != null && distanceKm <= radiusKm * 0.25;
    }

    private boolean isHighlyRated(BigDecimal ratingAvg) {
        return ratingAvg != null && ratingAvg.compareTo(RATING_REASON_THRESHOLD) >= 0;
    }

    private List<String> parseFinalTechniques(String finalTechniques) {
        if (finalTechniques == null || finalTechniques.isBlank()) {
            return List.of();
        }
        Set<String> techniques = new LinkedHashSet<>();
        try {
            JsonNode node = objectMapper.readTree(finalTechniques);
            if (!node.isArray()) {
                return List.of();
            }
            for (JsonNode item : node) {
                String technique = item.asText("").trim().toLowerCase(Locale.ROOT);
                if (!technique.isBlank()) {
                    techniques.add(technique);
                }
            }
            return List.copyOf(techniques);
        } catch (Exception e) {
            return List.of();
        }
    }

    private void validateRequest(double lat, double lng, double radiusKm, int size) {
        if (!Double.isFinite(lat) || lat < MIN_LAT || lat > MAX_LAT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "위도는 -90~90 범위여야 합니다.");
        }
        if (!Double.isFinite(lng) || lng < MIN_LNG || lng > MAX_LNG) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "경도는 -180~180 범위여야 합니다.");
        }
        if (!Double.isFinite(radiusKm) || radiusKm <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "반경은 0보다 커야 합니다.");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1~100 범위여야 합니다.");
        }
    }
}
