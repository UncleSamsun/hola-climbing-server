package com.holaclimbing.server.domain.admin.dto.request;

import com.holaclimbing.server.domain.gym.dto.DayHours;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record AdminGymUpsertRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 200) String address,
        Double lat,
        Double lng,
        @Size(max = 30) String phone,
        @Size(max = 300) String website,
        String description,
        Map<String, DayHours> businessHours,
        @Size(max = 20) String regionCode
) {
}
