package com.holaclimbing.server.domain.video.service;

import com.holaclimbing.server.domain.video.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    private final VideoMapper videoMapper;
    // TODO: Video 서비스 구현
}
