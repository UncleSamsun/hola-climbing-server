package com.holaclimbing.server.domain.gym.dto.response;

import com.holaclimbing.server.domain.gym.domain.GymGrade;

public record GymGradeResponse(
        Long id,
        Long gymId,
        String label,
        int difficultyOrder
) {
    public static GymGradeResponse from(GymGrade grade) {
        return new GymGradeResponse(
                grade.getId(), grade.getGymId(), grade.getLabel(),
                grade.getDifficultyOrder());
    }
}
