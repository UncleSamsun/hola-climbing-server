package com.holaclimbing.server.domain.user.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

abstract class AbstractOAuthProviderClient implements OAuthProviderClient {

    private final RestClient restClient;
    private final OAuthProperties properties;

    protected AbstractOAuthProviderClient(RestClient.Builder builder, OAuthProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    protected String exchangeCodeForAccessToken(OAuthAuthorizationCodeRequest request) {
        OAuthProperties.Provider providerProperties = properties.provider(request.provider());
        if (providerProperties == null || isBlank(providerProperties.clientId()) || isBlank(providerProperties.tokenUri())) {
            throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", providerProperties.clientId());
        if (!isBlank(providerProperties.clientSecret())) {
            body.add("client_secret", providerProperties.clientSecret());
        }
        body.add("code", request.code());
        body.add("redirect_uri", request.redirectUri());

        try {
            JsonNode response = restClient.post()
                    .uri(providerProperties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String accessToken = response == null ? null : response.path("access_token").asText(null);
            if (isBlank(accessToken)) {
                throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
            }
            return accessToken;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
        }
    }

    protected JsonNode getUserInfo(OAuthProvider provider, String accessToken) {
        OAuthProperties.Provider providerProperties = properties.provider(provider);
        if (providerProperties == null || isBlank(providerProperties.userInfoUri())) {
            throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
        }
        try {
            return restClient.get()
                    .uri(providerProperties.userInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
