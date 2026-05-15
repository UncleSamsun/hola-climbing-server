package com.holaclimbing.server.domain.stats.service;

import com.holaclimbing.server.domain.stats.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsMapper statsMapper;
    // TODO: Stats 서비스 구현
}
