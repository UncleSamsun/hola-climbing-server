package com.holaclimbing.server.domain.user.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class AppleOAuthProviderClientTest {

    private HttpServer server;
    private AppleOAuthProviderClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", this::handleToken);
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        OAuthProperties properties = new OAuthProperties(
                5,
                1,
                10,
                List.of("https://hola-climb.app/oauth/callback"),
                Map.of("apple", new OAuthProperties.Provider(
                        "test.apple.service",
                        "",
                        "https://appleid.apple.com/auth/authorize",
                        baseUrl + "/token",
                        "",
                        List.of("openid", "email", "name"),
                        "https://appleid.apple.com/auth/keys",
                        "form_post",
                        "TEAM1234567",
                        "KEY1234567",
                        "test-private-key",
                        30
                ))
        );
        client = new AppleOAuthProviderClient(
                RestClient.builder(),
                properties,
                new ObjectMapper(),
                new FixedAppleClientSecretGenerator(),
                new RecordingAppleIdTokenVerifier()
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchProfile_exchangesCodeVerifiesIdTokenAndUsesAppleUserJsonName() {
        OAuthUserProfile profile = client.fetchProfile(new OAuthAuthorizationCodeRequest(
                OAuthProvider.APPLE,
                "provider-code",
                "https://api.hola-climb.app/api/auth/oauth/apple/callback",
                "nonce-123",
                "{\"name\":{\"firstName\":\"Apple\",\"lastName\":\"Climber\"},\"email\":\"apple@hola.com\"}"
        ));

        assertThat(profile.provider()).isEqualTo(OAuthProvider.APPLE);
        assertThat(profile.providerId()).isEqualTo("apple-sub");
        assertThat(profile.email()).isEqualTo("apple@hola.com");
        assertThat(profile.nickname()).isEqualTo("Apple Climber");
        assertThat(profile.profileImage()).isNull();
    }

    @Test
    void fetchProfile_doesNotUseAppleUserJsonEmailWhenClaimEmailIsMissing() {
        OAuthUserProfile profile = client.fetchProfile(new OAuthAuthorizationCodeRequest(
                OAuthProvider.APPLE,
                "claims-email-missing-code",
                "https://api.hola-climb.app/api/auth/oauth/apple/callback",
                "nonce-123",
                "{\"name\":{\"firstName\":\"Payload\",\"lastName\":\"Only\"},\"email\":\"payload-only@hola.com\"}"
        ));

        assertThat(profile.provider()).isEqualTo(OAuthProvider.APPLE);
        assertThat(profile.providerId()).isEqualTo("apple-sub");
        assertThat(profile.email()).isNull();
        assertThat(profile.nickname()).isEqualTo("Payload Only");
        assertThat(profile.profileImage()).isNull();
    }

    @Test
    void fetchProfile_withoutIdTokenThrowsAuthorizationFailed() {
        assertThatThrownBy(() -> client.fetchProfile(new OAuthAuthorizationCodeRequest(
                OAuthProvider.APPLE,
                "missing-id-token-code",
                "https://api.hola-climb.app/api/auth/oauth/apple/callback",
                "nonce-123",
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        assertThat(exchange.getRequestMethod()).isEqualTo("POST");
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(requestBody).contains("grant_type=authorization_code");
        assertThat(requestBody).contains("client_id=test.apple.service");
        assertThat(requestBody).contains("client_secret=generated-apple-client-secret");
        assertThat(requestBody).contains(
                "redirect_uri=https%3A%2F%2Fapi.hola-climb.app%2Fapi%2Fauth%2Foauth%2Fapple%2Fcallback");

        if (requestBody.contains("code=missing-id-token-code")) {
            respond(exchange, 200, """
                    {"access_token":"apple-access","token_type":"Bearer","expires_in":3600}
                    """);
            return;
        }

        if (requestBody.contains("code=claims-email-missing-code")) {
            respond(exchange, 200, """
                    {"access_token":"apple-access","token_type":"Bearer","expires_in":3600,"id_token":"apple-id-token-without-email"}
                    """);
            return;
        }

        assertThat(requestBody).contains("code=provider-code");

        respond(exchange, 200, """
                {"access_token":"apple-access","token_type":"Bearer","expires_in":3600,"id_token":"apple-id-token"}
                """);
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static class FixedAppleClientSecretGenerator extends AppleClientSecretGenerator {

        FixedAppleClientSecretGenerator() {
            super(new ApplePrivateKeyParser());
        }

        @Override
        public String generate(OAuthProperties.Provider provider) {
            assertThat(provider.clientId()).isEqualTo("test.apple.service");
            return "generated-apple-client-secret";
        }
    }

    private static class RecordingAppleIdTokenVerifier extends AppleIdTokenVerifier {

        RecordingAppleIdTokenVerifier() {
            super(new AppleJwksClient(RestClient.builder(), new ObjectMapper()));
        }

        @Override
        public AppleIdTokenClaims verify(String idToken, OAuthProperties.Provider provider, String expectedNonce) {
            assertThat(idToken).isIn("apple-id-token", "apple-id-token-without-email");
            assertThat(provider.clientId()).isEqualTo("test.apple.service");
            assertThat(expectedNonce).isEqualTo("nonce-123");
            if ("apple-id-token-without-email".equals(idToken)) {
                return new AppleIdTokenClaims("apple-sub", null, true);
            }
            return new AppleIdTokenClaims("apple-sub", "apple@hola.com", true);
        }
    }
}
