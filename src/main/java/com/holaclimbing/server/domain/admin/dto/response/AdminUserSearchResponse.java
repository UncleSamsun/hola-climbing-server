package com.holaclimbing.server.domain.admin.dto.response;

import com.holaclimbing.server.domain.user.domain.User;

import java.time.LocalDateTime;

public record AdminUserSearchResponse(
        Long id,
        String email,
        String nickname,
        String role,
        String status,
        boolean emailVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {

    public static AdminUserSearchResponse from(User user) {
        return new AdminUserSearchResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
