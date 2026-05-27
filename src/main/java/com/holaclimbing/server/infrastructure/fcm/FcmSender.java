package com.holaclimbing.server.infrastructure.fcm;

import java.util.List;
import java.util.Map;

/**
 * FCM 푸시 전송 인터페이스. 운영 환경에서는 FirebaseFcmSender, 그 외에는 NoopFcmSender.
 */
public interface FcmSender {

    /**
     * 다수의 디바이스 토큰에 동일 알림을 전송한다.
     * 실패하거나 invalid한 토큰은 구현체가 정리·로깅한다. 호출자는 결과를 신경 쓰지 않는다.
     */
    void send(List<String> tokens, String title, String body, Map<String, String> data);
}
