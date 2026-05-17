package com.holaclimbing.server.domain.notification.service;

import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.notification.dto.response.NotificationResponse;

public interface NotificationService {

    /** 영상에 댓글이 달렸을 때 영상 소유자에게 알림. */
    void notifyComment(Long videoOwnerId, Long commenterId, Long videoId);

    /** 댓글에 답글이 달렸을 때 부모 댓글 작성자에게 알림. */
    void notifyReply(Long parentAuthorId, Long replierId, Long replyCommentId);

    /** 영상에 좋아요가 눌렸을 때 영상 소유자에게 알림. */
    void notifyLike(Long videoOwnerId, Long likerId, Long videoId);

    /** 팔로우 당했을 때 대상에게 알림. */
    void notifyFollow(Long followedId, Long followerId);

    /** 내 알림 목록 조회. */
    PageResponse<NotificationResponse> getNotifications(Long userId, boolean unreadOnly, int page, int size);

    /** 미읽음 알림 개수. */
    long getUnreadCount(Long userId);

    /** 단건 읽음 처리. */
    void markRead(Long userId, Long notificationId);

    /** 모든 알림 읽음 처리. */
    void markAllRead(Long userId);

    /** 알림 삭제. */
    void deleteNotification(Long userId, Long notificationId);
}
