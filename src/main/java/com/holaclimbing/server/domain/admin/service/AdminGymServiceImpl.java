package com.holaclimbing.server.domain.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.admin.dto.request.AdminGymUpsertRequest;
import com.holaclimbing.server.domain.admin.dto.request.AdminReasonRequest;
import com.holaclimbing.server.domain.admin.dto.response.AdminGymSearchResponse;
import com.holaclimbing.server.domain.admin.mapper.AdminGymMapper;
import com.holaclimbing.server.domain.gym.domain.Gym;
import com.holaclimbing.server.domain.gym.dto.DayHours;
import com.holaclimbing.server.domain.gym.dto.response.GymDetailResponse;
import com.holaclimbing.server.domain.gym.dto.response.GymPhotoResponse;
import com.holaclimbing.server.domain.gym.mapper.GymMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGymServiceImpl implements AdminGymService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_CLOSED = "closed";
    private static final TypeReference<Map<String, DayHours>> BUSINESS_HOURS_TYPE =
            new TypeReference<>() {
            };

    private final AdminGymMapper adminGymMapper;
    private final GymMapper gymMapper;
    private final AdminAuditService adminAuditService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminGymSearchResponse> search(String status, String keyword, String regionCode,
                                                       int page, int size) {
        long total = adminGymMapper.countSearch(status, keyword, regionCode);
        var content = adminGymMapper.search(status, keyword, regionCode, size, page * size).stream()
                .map(AdminGymSearchResponse::from)
                .toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public GymDetailResponse getGym(Long gymId) {
        return toDetail(requireGymAnyStatus(gymId));
    }

    @Override
    @Transactional
    public GymDetailResponse createGym(Long adminId, AdminGymUpsertRequest request) {
        Gym gym = Gym.builder()
                .name(request.name())
                .address(request.address())
                .lat(request.lat())
                .lng(request.lng())
                .description(request.description())
                .phone(request.phone())
                .website(request.website())
                .businessHours(writeBusinessHours(request.businessHours()))
                .regionCode(request.regionCode())
                .status(STATUS_ACTIVE)
                .createdBy(adminId)
                .build();
        gymMapper.insertGym(gym);
        Gym after = requireGymAnyStatus(gym.getId());
        adminAuditService.record(adminId, "GYM_CREATE", "gym", gym.getId(), null, null, after);
        return toDetail(after);
    }

    @Override
    @Transactional
    public GymDetailResponse updateGym(Long adminId, Long gymId, AdminGymUpsertRequest request) {
        Gym before = requireGymAnyStatus(gymId);
        adminGymMapper.updateGym(gymId, request.name(), request.address(), request.lat(), request.lng(),
                request.phone(), request.website(), request.description(), writeBusinessHours(request.businessHours()),
                request.regionCode());
        Gym after = requireGymAnyStatus(gymId);
        adminAuditService.record(adminId, "GYM_UPDATE", "gym", gymId, null, before, after);
        return toDetail(after);
    }

    @Override
    @Transactional
    public GymDetailResponse approveGym(Long adminId, Long gymId, AdminReasonRequest request) {
        return changeStatus(adminId, gymId, STATUS_ACTIVE, "GYM_APPROVE", request.reason());
    }

    @Override
    @Transactional
    public GymDetailResponse rejectGym(Long adminId, Long gymId, AdminReasonRequest request) {
        return changeStatus(adminId, gymId, STATUS_CLOSED, "GYM_REJECT", request.reason());
    }

    @Override
    @Transactional
    public GymDetailResponse closeGym(Long adminId, Long gymId, AdminReasonRequest request) {
        return changeStatus(adminId, gymId, STATUS_CLOSED, "GYM_CLOSE", request.reason());
    }

    private GymDetailResponse changeStatus(Long adminId, Long gymId, String status, String action, String reason) {
        Gym before = requireGymAnyStatus(gymId);
        adminGymMapper.updateStatus(gymId, status);
        Gym after = requireGymAnyStatus(gymId);
        adminAuditService.record(adminId, action, "gym", gymId, reason, before, after);
        return toDetail(after);
    }

    private Gym requireGymAnyStatus(Long gymId) {
        Gym gym = adminGymMapper.findByIdAnyStatus(gymId);
        if (gym == null) {
            throw new BusinessException(ErrorCode.GYM_NOT_FOUND);
        }
        return gym;
    }

    private GymDetailResponse toDetail(Gym gym) {
        List<GymPhotoResponse> photos = gymMapper.findPhotosByGymId(gym.getId())
                .stream()
                .map(GymPhotoResponse::from)
                .toList();
        return GymDetailResponse.of(gym, parseBusinessHours(gym.getBusinessHours()), photos);
    }

    private String writeBusinessHours(Map<String, DayHours> businessHours) {
        if (businessHours == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(businessHours);
        } catch (Exception e) {
            throw new IllegalStateException("business_hours 직렬화 실패", e);
        }
    }

    private Map<String, DayHours> parseBusinessHours(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, BUSINESS_HOURS_TYPE);
        } catch (Exception e) {
            log.warn("business_hours 파싱 실패: {}", e.getMessage());
            return Map.of();
        }
    }
}
