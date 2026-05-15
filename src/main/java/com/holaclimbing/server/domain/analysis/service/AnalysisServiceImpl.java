package com.holaclimbing.server.domain.analysis.service;

import com.holaclimbing.server.domain.analysis.mapper.AnalysisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {
    private final AnalysisMapper analysisMapper;
    // TODO: Analysis 서비스 구현
}
