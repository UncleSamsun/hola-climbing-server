package com.holaclimbing.server.domain.video;

import static com.holaclimbing.server.common.exception.error.ErrorCode.INVALID_INPUT;
import static com.holaclimbing.server.common.exception.error.ErrorCode.VIDEO_NOT_ACCESSIBLE;
import static com.holaclimbing.server.common.exception.error.ErrorCode.VIDEO_NOT_FOUND;

import com.holaclimbing.server.common.exception.docs.ApiErrorCodes;
import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.video.dto.response.VideoRecommendedGymResponse;
import com.holaclimbing.server.domain.video.service.VideoGymRecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/videos/{videoId}/recommendations")
@RequiredArgsConstructor
@Validated
public class VideoRecommendationController {

    private final VideoGymRecommendationService recommendationService;

    @ApiErrorCodes({VIDEO_NOT_FOUND, VIDEO_NOT_ACCESSIBLE, INVALID_INPUT})
    @GetMapping("/gyms")
    public ApiResponse<List<VideoRecommendedGymResponse>> getRecommendedGyms(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Long viewerId,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") @Positive double radius,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size) {
        return ApiResponse.success(recommendationService.getRecommendedGyms(
                videoId, viewerId, lat, lng, radius, size));
    }
}
