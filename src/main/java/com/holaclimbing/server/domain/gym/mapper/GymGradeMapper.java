package com.holaclimbing.server.domain.gym.mapper;

import com.holaclimbing.server.domain.gym.domain.GymGrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GymGradeMapper {

    /** 활성 암장 난이도 목록을 난이도순으로 조회한다. */
    List<GymGrade> findActiveByGymId(Long gymId);

    /** 영상 등록 검증용: 특정 암장에 속한 활성 난이도 단건 조회. */
    GymGrade findActiveByGymAndId(@Param("gymId") Long gymId, @Param("gradeId") Long gradeId);
}
