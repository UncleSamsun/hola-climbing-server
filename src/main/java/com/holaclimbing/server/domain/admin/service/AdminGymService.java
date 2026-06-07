package com.holaclimbing.server.domain.admin.service;

import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.admin.dto.request.AdminGymUpsertRequest;
import com.holaclimbing.server.domain.admin.dto.request.AdminReasonRequest;
import com.holaclimbing.server.domain.admin.dto.response.AdminGymSearchResponse;
import com.holaclimbing.server.domain.gym.dto.response.GymDetailResponse;

public interface AdminGymService {

    PageResponse<AdminGymSearchResponse> search(String status, String keyword, String regionCode, int page, int size);

    GymDetailResponse getGym(Long gymId);

    GymDetailResponse createGym(Long adminId, AdminGymUpsertRequest request);

    GymDetailResponse updateGym(Long adminId, Long gymId, AdminGymUpsertRequest request);

    GymDetailResponse approveGym(Long adminId, Long gymId, AdminReasonRequest request);

    GymDetailResponse rejectGym(Long adminId, Long gymId, AdminReasonRequest request);

    GymDetailResponse closeGym(Long adminId, Long gymId, AdminReasonRequest request);
}
