package com.holaclimbing.server.domain.recommendation.domain;

import java.time.OffsetDateTime;
import java.util.List;

public record RecommendationFeedSnapshot(
        String snapshotId,
        Long userId,
        List<Long> videoIds,
        OffsetDateTime createdAt
) {
}
