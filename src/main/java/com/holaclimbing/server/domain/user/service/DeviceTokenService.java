package com.holaclimbing.server.domain.user.service;

import com.holaclimbing.server.domain.user.dto.request.RegisterDeviceTokenRequest;
import com.holaclimbing.server.domain.user.dto.request.UnregisterDeviceTokenRequest;

public interface DeviceTokenService {

    /** 본인 디바이스 토큰 등록(upsert). FCM 푸시 알림을 받기 위한 선결 절차. */
    void register(Long userId, RegisterDeviceTokenRequest request);

    /** 본인 디바이스 토큰 해제 (예: 로그아웃·앱 삭제 시). */
    void unregister(Long userId, UnregisterDeviceTokenRequest request);
}
