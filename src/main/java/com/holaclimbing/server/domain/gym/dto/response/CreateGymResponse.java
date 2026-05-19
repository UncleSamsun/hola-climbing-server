package com.holaclimbing.server.domain.gym.dto.response;

import com.holaclimbing.server.domain.gym.domain.Gym;

/**
 * 암장 등록 제안 응답. 제안 직후 status는 'pending'.
 */
public record CreateGymResponse(
        Long id,
        String name,
        String status
) {
    public static CreateGymResponse of(Gym gym) {
        return new CreateGymResponse(gym.getId(), gym.getName(), gym.getStatus());
    }
}
