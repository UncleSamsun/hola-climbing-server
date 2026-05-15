package com.holaclimbing.server.domain.notification.service;

import com.holaclimbing.server.domain.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;
    // TODO: Notification 서비스 구현
}
