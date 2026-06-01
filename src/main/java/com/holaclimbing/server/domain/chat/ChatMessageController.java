package com.holaclimbing.server.domain.chat;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.common.security.StompHandshakeInterceptor;
import com.holaclimbing.server.domain.chat.dto.request.SendMessageRequest;
import com.holaclimbing.server.domain.chat.dto.response.ChatMessageResponse;
import com.holaclimbing.server.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * STOMP 채팅 메시지 핸들러.
 * 클라이언트가 /app/gyms/{gymId}/chat로 발행하면 저장 후
 * /topic/gyms/{gymId}/chat 구독자에게 브로드캐스트한다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    /** 발신자 개인 큐 — 클라이언트는 {@code /user/queue/errors}를 구독해 에러를 받는다. */
    private static final String USER_ERROR_QUEUE = "/queue/errors";

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/gyms/{gymId}/chat")
    @SendTo("/topic/gyms/{gymId}/chat")
    public ChatMessageResponse send(@DestinationVariable Long gymId,
                                    SendMessageRequest request,
                                    SimpMessageHeaderAccessor headerAccessor) {
        Long userId = (Long) headerAccessor.getSessionAttributes()
                .get(StompHandshakeInterceptor.SESSION_USER_ID);
        return chatService.sendMessage(gymId, userId, request);
    }

    /**
     * 메시지 처리 실패(비멤버·빈 내용 등)는 브로드캐스트하지 않고 발신자 개인 큐로 에러를 보낸다.
     * 기존엔 단순 로그만 남겨 클라이언트가 거부 사실을 모르고 "전송 성공"으로 인식했다.
     */
    @MessageExceptionHandler(BusinessException.class)
    public void handleBusinessException(BusinessException e, SimpMessageHeaderAccessor headerAccessor) {
        log.warn("채팅 메시지 처리 실패: {}", e.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            return;
        }
        // 클라이언트는 /user/queue/errors를 구독한다. STOMP Principal을 따로 세팅하지 않으므로
        // 세션 id를 user 식별자로 사용해 같은 STOMP 세션에만 라우팅한다.
        var headers = org.springframework.messaging.simp.SimpMessageHeaderAccessor.create();
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(
                sessionId,
                USER_ERROR_QUEUE,
                ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()),
                headers.getMessageHeaders());
    }
}
