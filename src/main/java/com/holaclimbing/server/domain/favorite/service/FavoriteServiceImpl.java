package com.holaclimbing.server.domain.favorite.service;

import com.holaclimbing.server.domain.favorite.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteMapper favoriteMapper;
    // TODO: Favorite 서비스 구현
}
