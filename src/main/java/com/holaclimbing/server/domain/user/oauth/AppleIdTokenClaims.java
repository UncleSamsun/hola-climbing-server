package com.holaclimbing.server.domain.user.oauth;

public record AppleIdTokenClaims(
        String subject,
        String email,
        boolean emailVerified
) {
}
