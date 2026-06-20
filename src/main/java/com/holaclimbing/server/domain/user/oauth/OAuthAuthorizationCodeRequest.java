package com.holaclimbing.server.domain.user.oauth;

public record OAuthAuthorizationCodeRequest(
        OAuthProvider provider,
        String code,
        String redirectUri
) {
}
