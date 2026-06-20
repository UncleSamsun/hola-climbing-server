package com.holaclimbing.server.domain.user.dto.response;

public record OAuthLoginResponse(
        Status status,
        boolean signupRequired,
        TokenResponse token,
        String signupToken,
        String email,
        String suggestedNickname,
        String profileImage
) {
    public static OAuthLoginResponse loggedIn(TokenResponse token) {
        return new OAuthLoginResponse(Status.LOGGED_IN, false, token, null, null, null, null);
    }

    public static OAuthLoginResponse signupRequired(
            String signupToken,
            String email,
            String suggestedNickname,
            String profileImage
    ) {
        return new OAuthLoginResponse(Status.SIGNUP_REQUIRED, true, null, signupToken, email, suggestedNickname, profileImage);
    }

    public static OAuthLoginResponse emailAlreadyExists(
            String email,
            String suggestedNickname,
            String profileImage
    ) {
        return new OAuthLoginResponse(Status.EMAIL_ALREADY_EXISTS, false, null, null, email, suggestedNickname, profileImage);
    }

    public enum Status {
        LOGGED_IN,
        SIGNUP_REQUIRED,
        EMAIL_ALREADY_EXISTS
    }
}
