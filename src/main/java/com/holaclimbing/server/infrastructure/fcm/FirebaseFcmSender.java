package com.holaclimbing.server.infrastructure.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.holaclimbing.server.domain.user.mapper.DeviceTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Firebase Admin SDK로 FCM 푸시를 전송하는 실제 구현체.
 * 500개씩 끊어 sendEachForMulticast로 전송하고, invalid 응답이 온 토큰은
 * DeviceTokenMapper로 즉시 정리한다.
 */
@Slf4j
@RequiredArgsConstructor
public class FirebaseFcmSender implements FcmSender {

    private static final int BATCH_SIZE = 500;

    private final DeviceTokenMapper deviceTokenMapper;

    @Override
    public void send(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        for (int i = 0; i < tokens.size(); i += BATCH_SIZE) {
            List<String> chunk = tokens.subList(i, Math.min(i + BATCH_SIZE, tokens.size()));
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data == null ? Map.of() : data)
                    .addAllTokens(chunk)
                    .build();
            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                cleanupInvalidTokens(chunk, response);
                log.info("[FCM] 전송 — success={}, failure={}",
                        response.getSuccessCount(), response.getFailureCount());
            } catch (Exception e) {
                log.warn("[FCM] 전송 실패: {}", e.getMessage());
            }
        }
    }

    /** invalid/unregistered 응답이 온 토큰을 DB에서 제거한다. */
    private void cleanupInvalidTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful() || r.getException() == null) continue;
            String code = r.getException().getMessagingErrorCode() != null
                    ? r.getException().getMessagingErrorCode().name() : "";
            if ("UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code)) {
                String dead = tokens.get(i);
                int removed = deviceTokenMapper.deleteByToken(dead);
                log.info("[FCM] invalid 토큰 정리 — code={}, removed={}", code, removed);
            }
        }
    }
}
