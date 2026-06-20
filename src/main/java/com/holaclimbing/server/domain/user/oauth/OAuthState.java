package com.holaclimbing.server.domain.user.oauth;

public record OAuthState(
        String provider,
        String redirectUri
) {
    public static OAuthState of(OAuthProvider provider, String redirectUri) {
        return new OAuthState(provider.value(), redirectUri);
    }

    public OAuthProvider providerEnum() {
        return OAuthProvider.from(provider);
    }
}
