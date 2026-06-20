package com.holaclimbing.server.domain.user.oauth;

public record OAuthUserProfile(
        OAuthProvider provider,
        String providerId,
        String email,
        String nickname,
        String profileImage
) {
}
