package com.holaclimbing.server.domain.user.oauth;

public record OAuthPendingSignup(
        String provider,
        String providerId,
        String email,
        String nickname,
        String profileImage
) {
    public static OAuthPendingSignup from(OAuthUserProfile profile) {
        return new OAuthPendingSignup(
                profile.provider().value(),
                profile.providerId(),
                profile.email(),
                profile.nickname(),
                profile.profileImage()
        );
    }

    public OAuthUserProfile toProfile() {
        return new OAuthUserProfile(
                OAuthProvider.from(provider),
                providerId,
                email,
                nickname,
                profileImage
        );
    }
}
