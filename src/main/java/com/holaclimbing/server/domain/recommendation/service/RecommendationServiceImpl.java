package com.holaclimbing.server.domain.recommendation.service;

import com.holaclimbing.server.common.response.CursorPageResponse;
import com.holaclimbing.server.domain.recommendation.dto.RecommendationCursor;
import com.holaclimbing.server.domain.recommendation.dto.RecommendationCursorCodec;
import com.holaclimbing.server.domain.recommendation.dto.response.RecommendedVideoResponse;
import com.holaclimbing.server.domain.recommendation.mapper.RecommendationMapper;
import com.holaclimbing.server.domain.video.domain.Video;
import com.holaclimbing.server.infrastructure.gcs.GcsStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final String SOURCE_FOLLOWING = "following";
    private static final String SOURCE_RECOMMENDED = "recommended";

    private final RecommendationMapper recommendationMapper;
    private final GcsStorageService gcsStorageService;

    @Override
    public CursorPageResponse<RecommendedVideoResponse> getVideoFeed(Long userId, String cursor, int size) {
        RecommendationCursor decodedCursor = RecommendationCursorCodec.decode(cursor);
        List<Video> videos = recommendationMapper.findFeedVideos(userId, decodedCursor, size + 1);
        boolean hasNext = videos.size() > size;
        List<Video> pageVideos = hasNext ? videos.subList(0, size) : videos;

        List<RecommendedVideoResponse> content = pageVideos
                .stream()
                .map(v -> RecommendedVideoResponse.of(v,
                        gcsStorageService.createReadUrl(v.getGcsPath()),
                        Integer.valueOf(1).equals(v.getFollowingRank()) ? SOURCE_FOLLOWING : SOURCE_RECOMMENDED))
                .toList();

        String nextCursor = hasNext
                ? RecommendationCursorCodec.encode(RecommendationCursor.from(pageVideos.get(pageVideos.size() - 1)))
                : null;
        return CursorPageResponse.of(content, nextCursor, hasNext);
    }
}
