package com.holaclimbing.server.domain.user.oauth;

public record OAuthState(
        String provider,
        String redirectUri,
        String backendRedirectUri
) {
    public static OAuthState of(OAuthProvider provider, String redirectUri, String backendRedirectUri) {
        return new OAuthState(provider.value(), redirectUri, backendRedirectUri);
    }

    public OAuthProvider providerEnum() {
        return OAuthProvider.from(provider);
    }
}
