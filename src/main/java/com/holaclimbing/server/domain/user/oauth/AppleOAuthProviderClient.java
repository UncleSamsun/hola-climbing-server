package com.holaclimbing.server.domain.user.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AppleOAuthProviderClient extends AbstractOAuthProviderClient {

    private final ObjectMapper objectMapper;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final AppleIdTokenVerifier idTokenVerifier;

    public AppleOAuthProviderClient(
            RestClient.Builder builder,
            OAuthProperties properties,
            ObjectMapper objectMapper,
            AppleClientSecretGenerator clientSecretGenerator,
            AppleIdTokenVerifier idTokenVerifier
    ) {
        super(builder, properties, objectMapper);
        this.objectMapper = objectMapper;
        this.clientSecretGenerator = clientSecretGenerator;
        this.idTokenVerifier = idTokenVerifier;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.APPLE;
    }

    @Override
    public OAuthUserProfile fetchProfile(OAuthAuthorizationCodeRequest request) {
        OAuthProperties.Provider providerProperties = providerProperties(provider());
        JsonNode tokenResponse = exchangeCodeForTokenResponse(request);
        String idToken = tokenResponse == null ? null : tokenResponse.path("id_token").asText(null);
        if (isBlank(idToken)) {
            throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
        }

        AppleIdTokenClaims claims = idTokenVerifier.verify(idToken, providerProperties, request.nonce());
        AppleUserPayload userPayload = parseAppleUserPayload(request.providerUserJson());

        return new OAuthUserProfile(
                provider(),
                claims.subject(),
                claims.email(),
                userPayload.fullName(),
                null
        );
    }

    @Override
    protected String clientSecretValue(
            OAuthAuthorizationCodeRequest request,
            OAuthProperties.Provider providerProperties
    ) {
        return clientSecretGenerator.generate(providerProperties);
    }

    private AppleUserPayload parseAppleUserPayload(String providerUserJson) {
        if (isBlank(providerUserJson)) {
            return AppleUserPayload.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(providerUserJson);
            JsonNode name = root.path("name");
            String fullName = fullName(name.path("firstName").asText(null), name.path("lastName").asText(null));
            return new AppleUserPayload(fullName);
        } catch (JsonProcessingException e) {
            return AppleUserPayload.empty();
        }
    }

    private String fullName(String firstName, String lastName) {
        String first = trimToNull(firstName);
        String last = trimToNull(lastName);
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return first + " " + last;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record AppleUserPayload(String fullName) {

        private static AppleUserPayload empty() {
            return new AppleUserPayload(null);
        }
    }
}
