package com.holaclimbing.server.domain.notification.domain;

import lombok.Getter;

/**
 * 알림 종류. code는 notifications.type 컬럼에 저장되는 값.
 */
@Getter
public enum NotificationType {

    COMMENT("comment", "새 댓글", "회원님의 영상에 새 댓글이 달렸습니다."),
    REPLY("reply", "새 답글", "회원님의 댓글에 답글이 달렸습니다."),
    LIKE("like", "좋아요", "회원님의 영상을 좋아합니다."),
    FOLLOW("follow", "새 팔로워", "회원님을 팔로우하기 시작했습니다."),
    SYSTEM("system", "안내", null);

    private final String code;
    private final String title;
    private final String defaultContent;

    NotificationType(String code, String title, String defaultContent) {
        this.code = code;
        this.title = title;
        this.defaultContent = defaultContent;
    }
}
