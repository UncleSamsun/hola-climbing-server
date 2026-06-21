package com.holaclimbing.server.domain.gym.dto.response;

import com.holaclimbing.server.domain.gym.domain.GymReview;

import java.time.OffsetDateTime;

public record GymReviewResponse(
        Long id,
        Long gymId,
        Long userId,
        String nickname,
        String profileImage,
        int rating,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static GymReviewResponse of(GymReview review) {
        return of(review, review.getProfileImage());
    }

    public static GymReviewResponse of(GymReview review, String profileImage) {
        return new GymReviewResponse(
                review.getId(), review.getGymId(), review.getUserId(),
                review.getNickname(), profileImage, review.getRating(),
                review.getContent(), review.getCreatedAt(), review.getUpdatedAt());
    }
}
