package com.holaclimbing.server.domain.report.service;

import com.holaclimbing.server.domain.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportMapper reportMapper;
    // TODO: Report 서비스 구현
}
