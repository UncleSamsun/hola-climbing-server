package com.holaclimbing.server.domain.user.service;

import com.holaclimbing.server.domain.user.domain.DeviceToken;
import com.holaclimbing.server.domain.user.dto.request.RegisterDeviceTokenRequest;
import com.holaclimbing.server.domain.user.dto.request.UnregisterDeviceTokenRequest;
import com.holaclimbing.server.domain.user.mapper.DeviceTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenMapper deviceTokenMapper;

    @Override
    @Transactional
    public void register(Long userId, RegisterDeviceTokenRequest request) {
        deviceTokenMapper.upsert(DeviceToken.builder()
                .userId(userId)
                .token(request.token())
                .platform(request.platform())
                .build());
    }

    @Override
    @Transactional
    public void unregister(Long userId, UnregisterDeviceTokenRequest request) {
        deviceTokenMapper.deleteByUserAndToken(userId, request.token());
    }
}
