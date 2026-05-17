package com.holaclimbing.server.domain.notification;

import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.notification.dto.response.NotificationResponse;
import com.holaclimbing.server.domain.notification.service.NotificationService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 API. 모두 인증이 필요한 본인 전용 엔드포인트.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Positive int size) {
        return ApiResponse.success(notificationService.getNotifications(userId, unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal Long userId,
                                      @PathVariable Long notificationId) {
        notificationService.markRead(userId, notificationId);
        return ApiResponse.success();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllRead(userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long notificationId) {
        notificationService.deleteNotification(userId, notificationId);
        return ApiResponse.success();
    }
}
