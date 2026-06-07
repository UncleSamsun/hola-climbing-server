package com.holaclimbing.server.domain.admin.dto.response;

import com.holaclimbing.server.domain.user.domain.User;

import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long id,
        String email,
        String nickname,
        String profileImage,
        String bio,
        String role,
        String status,
        boolean emailVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage(),
                user.getBio(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
