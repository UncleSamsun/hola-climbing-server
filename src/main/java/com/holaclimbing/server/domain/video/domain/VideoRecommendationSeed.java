package com.holaclimbing.server.domain.video.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecommendationSeed {

    private Long id;
    private Long userId;
    private boolean isPublic;
    private String finalTechniques;
    private Boolean finalIsDynamic;
}
