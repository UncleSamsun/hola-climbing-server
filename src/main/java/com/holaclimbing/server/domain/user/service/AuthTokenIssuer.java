package com.holaclimbing.server.domain.user.service;

import com.holaclimbing.server.common.security.JwtTokenProvider;
import com.holaclimbing.server.domain.user.domain.User;
import com.holaclimbing.server.domain.user.dto.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final JwtTokenProvider tokenProvider;

    public TokenResponse issue(User user) {
        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());
        return TokenResponse.of(accessToken, refreshToken);
    }
}
