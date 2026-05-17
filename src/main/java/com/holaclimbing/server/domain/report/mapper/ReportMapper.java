package com.holaclimbing.server.domain.report.mapper;

import com.holaclimbing.server.domain.report.domain.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportMapper {

    /** 신고 저장. 생성된 PK는 report.id로 채워진다. status는 DB 기본값 'pending'. */
    void insert(Report report);

    /** 신고 단건 조회. 없으면 null. */
    Report findById(Long id);

    /** 같은 신고자가 같은 대상을 이미 신고했는지 여부. */
    boolean existsByReporterAndTarget(@Param("reporterId") Long reporterId,
                                      @Param("targetType") String targetType,
                                      @Param("targetId") Long targetId);
}
