package com.holaclimbing.server.domain.recommendation.dto;

import com.holaclimbing.server.domain.video.domain.Video;
import java.time.OffsetDateTime;

public class RecommendationCursor {

    private final String snapshotId;
    private final Integer offset;
    private final int distanceNullRank;
    private final Double rankingDistance;
    private final int followingRank;
    private final OffsetDateTime createdAt;
    private final Long id;

    public RecommendationCursor(
            int distanceNullRank,
            Double rankingDistance,
            int followingRank,
            OffsetDateTime createdAt,
            Long id) {
        this.snapshotId = null;
        this.offset = null;
        this.distanceNullRank = distanceNullRank;
        this.rankingDistance = rankingDistance;
        this.followingRank = followingRank;
        this.createdAt = createdAt;
        this.id = id;
    }

    private RecommendationCursor(String snapshotId, int offset) {
        this.snapshotId = snapshotId;
        this.offset = offset;
        this.distanceNullRank = 0;
        this.rankingDistance = null;
        this.followingRank = 0;
        this.createdAt = null;
        this.id = null;
    }

    public static RecommendationCursor from(Video video) {
        return new RecommendationCursor(
                video.getDistanceNullRank(),
                video.getRankingDistance(),
                video.getFollowingRank(),
                video.getCreatedAt(),
                video.getId());
    }

    public static RecommendationCursor snapshot(String snapshotId, int offset) {
        return new RecommendationCursor(snapshotId, offset);
    }

    public boolean isSnapshot() {
        return snapshotId != null;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public int getOffset() {
        return offset == null ? 0 : offset;
    }

    public int getDistanceNullRank() {
        return distanceNullRank;
    }

    public Double getRankingDistance() {
        return rankingDistance;
    }

    public int getFollowingRank() {
        return followingRank;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getId() {
        return id;
    }
}
