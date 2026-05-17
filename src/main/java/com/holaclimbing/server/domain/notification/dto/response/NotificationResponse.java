package com.holaclimbing.server.domain.notification.dto.response;

import com.holaclimbing.server.domain.notification.domain.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String targetType,
        Long targetId,
        Long senderId,
        String title,
        String content,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTargetType(), n.getTargetId(), n.getSenderId(),
                n.getTitle(), n.getContent(), n.isRead(), n.getCreatedAt());
    }
}
