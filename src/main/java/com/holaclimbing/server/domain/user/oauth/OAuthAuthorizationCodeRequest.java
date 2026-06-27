package com.holaclimbing.server.domain.user.oauth;

public record OAuthAuthorizationCodeRequest(
        OAuthProvider provider,
        String code,
        String redirectUri,
        String nonce,
        String providerUserJson
) {
    public OAuthAuthorizationCodeRequest(OAuthProvider provider, String code, String redirectUri) {
        this(provider, code, redirectUri, null, null);
    }
}
