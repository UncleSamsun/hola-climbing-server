package com.holaclimbing.server.domain.chat.dto.request;

/**
 * STOMP 채팅 메시지 발행 요청. content 검증은 서비스 계층에서 수행한다.
 * lat/lng는 선택값 — 함께 보내면 서버가 암장 300m 반경 내 작성 여부를 판정한다.
 */
public record SendMessageRequest(
        String content,
        Double lat,
        Double lng
) {
}
