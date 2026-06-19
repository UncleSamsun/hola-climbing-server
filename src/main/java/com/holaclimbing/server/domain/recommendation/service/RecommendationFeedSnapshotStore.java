package com.holaclimbing.server.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaclimbing.server.domain.recommendation.domain.RecommendationFeedSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecommendationFeedSnapshotStore {

    private static final String KEY_PREFIX = "recommendation:feed:snapshot:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public String save(Long userId, List<Long> videoIds) {
        String snapshotId = UUID.randomUUID().toString();
        RecommendationFeedSnapshot snapshot = new RecommendationFeedSnapshot(
                snapshotId,
                userId,
                List.copyOf(videoIds),
                OffsetDateTime.now());
        try {
            redis.opsForValue().set(key(userId, snapshotId), objectMapper.writeValueAsString(snapshot), TTL);
            return snapshotId;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("추천 피드 스냅샷 저장에 실패했습니다.", e);
        }
    }

    public Optional<RecommendationFeedSnapshot> find(Long userId, String snapshotId) {
        String json = redis.opsForValue().get(key(userId, snapshotId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            RecommendationFeedSnapshot snapshot = objectMapper.readValue(json, RecommendationFeedSnapshot.class);
            if (!userId.equals(snapshot.userId())) {
                return Optional.empty();
            }
            return Optional.of(snapshot);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private String key(Long userId, String snapshotId) {
        return KEY_PREFIX + userId + ":" + snapshotId;
    }
}
