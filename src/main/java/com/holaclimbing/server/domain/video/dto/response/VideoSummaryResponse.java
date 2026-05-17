package com.holaclimbing.server.domain.video.dto.response;

import com.holaclimbing.server.domain.video.domain.Video;

import java.time.LocalDateTime;

public record VideoSummaryResponse(
        Long id,
        Long userId,
        Long gymId,
        String title,
        String grade,
        String thumbnailPath,
        Integer durationSeconds,
        int viewCount,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt
) {
    public static VideoSummaryResponse from(Video video) {
        return new VideoSummaryResponse(
                video.getId(), video.getUserId(), video.getGymId(), video.getTitle(), video.getGrade(),
                video.getThumbnailPath(), video.getDurationSeconds(), video.getViewCount(),
                video.getLikeCount(), video.getCommentCount(), video.getCreatedAt());
    }
}
