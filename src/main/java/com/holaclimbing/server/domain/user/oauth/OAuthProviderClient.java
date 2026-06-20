package com.holaclimbing.server.domain.user.oauth;

public interface OAuthProviderClient {
    OAuthProvider provider();

    OAuthUserProfile fetchProfile(OAuthAuthorizationCodeRequest request);
}
