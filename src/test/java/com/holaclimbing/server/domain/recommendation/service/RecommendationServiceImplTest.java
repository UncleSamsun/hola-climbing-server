package com.holaclimbing.server.domain.recommendation.service;

import com.holaclimbing.server.domain.gym.service.GymProfileImageUrlResolver;
import com.holaclimbing.server.domain.recommendation.mapper.RecommendationMapper;
import com.holaclimbing.server.infrastructure.gcs.GcsStorageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationServiceImplTest {

    private final RecommendationMapper recommendationMapper = mock(RecommendationMapper.class);
    private final GcsStorageService gcsStorageService = mock(GcsStorageService.class);
    private final GymProfileImageUrlResolver profileImageUrlResolver = mock(GymProfileImageUrlResolver.class);
    private final RecommendationServiceImpl service = new RecommendationServiceImpl(
            recommendationMapper,
            gcsStorageService,
            profileImageUrlResolver);

    @Test
    void getVideoFeedPassesDefaultCandidateWindowForNormalPageSize() {
        when(recommendationMapper.findFeedVideos(eq(42L), isNull(), eq(21), eq(5_000)))
                .thenReturn(List.of());

        service.getVideoFeed(42L, null, 20);

        verify(recommendationMapper).findFeedVideos(eq(42L), isNull(), eq(21), eq(5_000));
    }

    @Test
    void getVideoFeedScalesCandidateWindowForLargePageSize() {
        when(recommendationMapper.findFeedVideos(eq(42L), isNull(), eq(201), eq(10_050)))
                .thenReturn(List.of());

        service.getVideoFeed(42L, null, 200);

        verify(recommendationMapper).findFeedVideos(eq(42L), isNull(), eq(201), eq(10_050));
    }
}
