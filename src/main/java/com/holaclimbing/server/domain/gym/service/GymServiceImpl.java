package com.holaclimbing.server.domain.gym.service;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.gym.domain.Gym;
import com.holaclimbing.server.domain.gym.domain.GymPhoto;
import com.holaclimbing.server.domain.gym.dto.request.CreateGymPhotoRequest;
import com.holaclimbing.server.domain.gym.dto.request.CreateGymRequest;
import com.holaclimbing.server.domain.gym.dto.response.CreateGymResponse;
import com.holaclimbing.server.domain.gym.dto.response.GymDetailResponse;
import com.holaclimbing.server.domain.gym.dto.response.GymPhotoResponse;
import com.holaclimbing.server.domain.gym.dto.response.GymSummaryResponse;
import com.holaclimbing.server.domain.gym.mapper.GymMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GymServiceImpl implements GymService {

    private final GymMapper gymMapper;

    @Override
    public PageResponse<GymSummaryResponse> searchGyms(String keyword, String region, int page, int size) {
        long total = gymMapper.countSearch(keyword, region);
        List<GymSummaryResponse> content = gymMapper.search(keyword, region, size, page * size)
                .stream().map(GymSummaryResponse::from).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public List<GymSummaryResponse> findNearbyGyms(double lat, double lng, double radiusKm, int size) {
        if (lat < -90 || lat > 90) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "위도는 -90~90 범위여야 합니다.");
        }
        if (lng < -180 || lng > 180) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "경도는 -180~180 범위여야 합니다.");
        }
        return gymMapper.findNearby(lat, lng, radiusKm, size)
                .stream().map(GymSummaryResponse::from).toList();
    }

    @Override
    public GymDetailResponse getGymDetail(Long gymId) {
        Gym gym = gymMapper.findById(gymId);
        if (gym == null) {
            throw new BusinessException(ErrorCode.GYM_NOT_FOUND);
        }
        List<GymPhotoResponse> photos = gymMapper.findPhotosByGymId(gymId)
                .stream().map(GymPhotoResponse::from).toList();
        return GymDetailResponse.of(gym, photos);
    }

    @Override
    @Transactional
    public CreateGymResponse suggestGym(Long userId, CreateGymRequest request) {
        Gym gym = Gym.builder()
                .name(request.name())
                .address(request.address())
                .lat(request.lat())
                .lng(request.lng())
                .description(request.description())
                .phone(request.phone())
                .website(request.website())
                .regionCode(request.regionCode())
                .status("pending")
                .createdBy(userId)
                .build();
        gymMapper.insertGym(gym);
        return CreateGymResponse.of(gym);
    }

    @Override
    @Transactional
    public GymPhotoResponse uploadPhoto(Long userId, Long gymId, CreateGymPhotoRequest request) {
        requireGym(gymId);
        GymPhoto photo = GymPhoto.builder()
                .gymId(gymId)
                .uploadedBy(userId)
                .gcsPath(request.gcsPath())
                .caption(request.caption())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .build();
        gymMapper.insertPhoto(photo);
        return GymPhotoResponse.from(photo);
    }

    @Override
    public List<GymPhotoResponse> getPhotos(Long gymId) {
        requireGym(gymId);
        return gymMapper.findPhotosByGymId(gymId)
                .stream().map(GymPhotoResponse::from).toList();
    }

    private void requireGym(Long gymId) {
        if (gymMapper.findById(gymId) == null) {
            throw new BusinessException(ErrorCode.GYM_NOT_FOUND);
        }
    }
}
