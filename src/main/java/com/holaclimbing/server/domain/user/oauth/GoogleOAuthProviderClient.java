package com.holaclimbing.server.domain.user.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleOAuthProviderClient extends AbstractOAuthProviderClient {

    public GoogleOAuthProviderClient(RestClient.Builder builder, OAuthProperties properties) {
        super(builder, properties);
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserProfile fetchProfile(OAuthAuthorizationCodeRequest request) {
        String accessToken = exchangeCodeForAccessToken(request);
        JsonNode userInfo = getUserInfo(provider(), accessToken);
        return new OAuthUserProfile(
                provider(),
                userInfo.path("sub").asText(),
                userInfo.path("email").asText(null),
                userInfo.path("name").asText(null),
                userInfo.path("picture").asText(null)
        );
    }
}
