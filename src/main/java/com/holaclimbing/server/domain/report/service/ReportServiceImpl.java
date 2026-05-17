package com.holaclimbing.server.domain.report.service;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import com.holaclimbing.server.domain.report.domain.Report;
import com.holaclimbing.server.domain.report.dto.request.CreateReportRequest;
import com.holaclimbing.server.domain.report.dto.response.ReportResponse;
import com.holaclimbing.server.domain.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Set<String> TARGET_TYPES =
            Set.of("video", "comment", "user", "gym", "chat_message");
    private static final Set<String> REASON_CODES =
            Set.of("spam", "abuse", "sexual", "copyright", "other");

    private final ReportMapper reportMapper;

    @Override
    @Transactional
    public ReportResponse createReport(Long reporterId, CreateReportRequest request) {
        if (!TARGET_TYPES.contains(request.targetType())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 신고 대상 유형입니다.");
        }
        if (!REASON_CODES.contains(request.reasonCode())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 신고 사유입니다.");
        }
        if (reportMapper.existsByReporterAndTarget(reporterId, request.targetType(), request.targetId())) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }
        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reasonCode(request.reasonCode())
                .reasonDetail(request.reasonDetail())
                .build();
        reportMapper.insert(report);
        return ReportResponse.of(reportMapper.findById(report.getId()));
    }
}
