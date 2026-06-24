package com.holaclimbing.server.domain.notification.event;

import com.holaclimbing.server.domain.notification.domain.NotificationType;

/**
 * 댓글·답글·좋아요·팔로우 등 도메인 행동이 알림 생성을 요청할 때 발행된다.
 */
public record NotificationRequestedEvent(
        Long recipientId,
        Long senderId,
        NotificationType type,
        String targetType,
        Long targetId
) {
}
