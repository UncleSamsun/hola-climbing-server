package com.holaclimbing.server.infrastructure.fcm;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * FCM 비활성화 시 사용되는 기본 구현. 호출 시 디버그 로그만 남기고 아무것도 보내지 않는다.
 * 개발·테스트 환경에서 FirebaseApp 초기화 없이 Bean 그래프가 완성되도록 한다.
 */
@Slf4j
public class NoopFcmSender implements FcmSender {

    @Override
    public void send(List<String> tokens, String title, String body, Map<String, String> data) {
        log.debug("[FCM-Noop] {}건 전송 생략 — title={}, data={}", tokens.size(), title, data);
    }
}
