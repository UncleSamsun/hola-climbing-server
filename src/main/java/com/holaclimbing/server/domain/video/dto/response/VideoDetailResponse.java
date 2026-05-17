package com.holaclimbing.server.domain.video.dto.response;

import com.holaclimbing.server.domain.video.domain.Video;

import java.time.LocalDateTime;

public record VideoDetailResponse(
        Long id,
        Long userId,
        Long gymId,
        String title,
        String description,
        String grade,
        String gcsPath,
        String gcsStreamingPath,
        String thumbnailPath,
        Integer durationSeconds,
        String status,
        boolean isPublic,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean isLiked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VideoDetailResponse of(Video video, boolean isLiked) {
        return new VideoDetailResponse(
                video.getId(), video.getUserId(), video.getGymId(), video.getTitle(),
                video.getDescription(), video.getGrade(), video.getGcsPath(),
                video.getGcsStreamingPath(), video.getThumbnailPath(), video.getDurationSeconds(),
                video.getStatus(), video.isPublic(), video.getViewCount(), video.getLikeCount(),
                video.getCommentCount(), isLiked, video.getCreatedAt(), video.getUpdatedAt());
    }
}
