package com.holaclimbing.server.domain.recommendation.service;

import com.holaclimbing.server.domain.recommendation.mapper.RecommendationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    private final RecommendationMapper recommendationMapper;
    // TODO: Recommendation 서비스 구현
}
