package com.holaclimbing.server.domain.notification.event;

import com.holaclimbing.server.domain.notification.domain.Notification;
import com.holaclimbing.server.domain.notification.mapper.NotificationMapper;
import com.holaclimbing.server.domain.user.mapper.DeviceTokenMapper;
import com.holaclimbing.server.infrastructure.fcm.FcmSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationMapper notificationMapper;
    private final DeviceTokenMapper deviceTokenMapper;
    private final FcmSender fcmSender;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(NotificationRequestedEvent event) {
        try {
            Notification notification = Notification.builder()
                    .recipientId(event.recipientId())
                    .senderId(event.senderId())
                    .type(event.type().getCode())
                    .targetType(event.targetType())
                    .targetId(event.targetId())
                    .title(event.type().getTitle())
                    .content(event.type().getDefaultContent())
                    .build();
            notificationMapper.insert(notification);
            sendPush(notification);
        } catch (Exception e) {
            log.warn("알림 생성/푸시 실패 — recipient={}, type={}: {}",
                    event.recipientId(), event.type().getCode(), e.getMessage());
        }
    }

    private void sendPush(Notification notification) {
        List<String> tokens = deviceTokenMapper.findTokensByUserId(notification.getRecipientId());
        if (tokens.isEmpty()) {
            return;
        }
        Map<String, String> data = Map.of(
                "notificationId", String.valueOf(notification.getId()),
                "type", notification.getType(),
                "targetType", notification.getTargetType(),
                "targetId", String.valueOf(notification.getTargetId())
        );
        fcmSender.send(tokens, notification.getTitle(), notification.getContent(), data);
    }
}
