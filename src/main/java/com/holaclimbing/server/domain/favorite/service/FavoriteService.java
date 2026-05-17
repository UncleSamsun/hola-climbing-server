package com.holaclimbing.server.domain.favorite.service;

import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.gym.dto.response.GymSummaryResponse;

public interface FavoriteService {

    /** 암장을 즐겨찾기에 추가. */
    void addFavorite(Long userId, Long gymId);

    /** 암장을 즐겨찾기에서 제거. */
    void removeFavorite(Long userId, Long gymId);

    /** 사용자가 즐겨찾기한 암장 목록. */
    PageResponse<GymSummaryResponse> getFavoriteGyms(Long userId, int page, int size);
}
