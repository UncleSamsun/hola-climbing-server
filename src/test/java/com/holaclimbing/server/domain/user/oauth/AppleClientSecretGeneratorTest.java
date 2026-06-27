package com.holaclimbing.server.domain.user.oauth;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleClientSecretGeneratorTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-27T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void generate_createsAppleClientSecretJwtSignedWithConfiguredPrivateKey() throws Exception {
        KeyPair keyPair = generateEcP256KeyPair();
        OAuthProperties.Provider provider = appleProvider(base64EncodedPem(keyPair), 30);
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(new ApplePrivateKeyParser(), FIXED_CLOCK);

        String clientSecret = generator.generate(provider);

        Jws<Claims> jwt = Jwts.parser()
                .verifyWith((ECPublicKey) keyPair.getPublic())
                .build()
                .parseSignedClaims(clientSecret);
        Claims claims = jwt.getPayload();
        assertThat(jwt.getHeader().getKeyId()).isEqualTo(provider.keyId());
        assertThat(claims.getIssuer()).isEqualTo(provider.teamId());
        assertThat(claims.getSubject()).isEqualTo(provider.clientId());
        assertThat(claims.getAudience()).contains("https://appleid.apple.com");
        assertThat(claims.getIssuedAt()).isEqualTo(Date.from(FIXED_INSTANT));
        assertThat(claims.getExpiration()).isEqualTo(Date.from(Instant.parse("2026-07-27T00:00:00Z")));
    }

    @Test
    void generate_missingProviderThrowsAuthorizationFailure() {
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(new ApplePrivateKeyParser(), FIXED_CLOCK);

        assertAuthorizationFailure(() -> generator.generate(null));
    }

    @Test
    void generate_blankRequiredProviderValuesThrowAuthorizationFailure() throws Exception {
        KeyPair keyPair = generateEcP256KeyPair();
        String privateKeyBase64 = base64EncodedPem(keyPair);
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(new ApplePrivateKeyParser(), FIXED_CLOCK);

        assertAuthorizationFailure(() -> generator.generate(appleProvider(" ", "TEAMID1234", "KEYID1234", privateKeyBase64, 30)));
        assertAuthorizationFailure(() -> generator.generate(appleProvider("com.hola.app", " ", "KEYID1234", privateKeyBase64, 30)));
        assertAuthorizationFailure(() -> generator.generate(appleProvider("com.hola.app", "TEAMID1234", " ", privateKeyBase64, 30)));
        assertAuthorizationFailure(() -> generator.generate(appleProvider("com.hola.app", "TEAMID1234", "KEYID1234", " ", 30)));
    }

    private static void assertAuthorizationFailure(ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED));
    }

    private static KeyPair generateEcP256KeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String base64EncodedPem(KeyPair keyPair) {
        String encodedKey = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + encodedKey
                + "\n-----END PRIVATE KEY-----\n";
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    private static OAuthProperties.Provider appleProvider(String privateKeyBase64, Integer ttlDays) {
        return appleProvider("com.hola.app", "TEAMID1234", "KEYID1234", privateKeyBase64, ttlDays);
    }

    private static OAuthProperties.Provider appleProvider(
            String clientId,
            String teamId,
            String keyId,
            String privateKeyBase64,
            Integer ttlDays
    ) {
        return new OAuthProperties.Provider(
                clientId,
                null,
                "https://appleid.apple.com/auth/authorize",
                "https://appleid.apple.com/auth/token",
                null,
                List.of("name", "email"),
                "https://appleid.apple.com/auth/keys",
                "form_post",
                teamId,
                keyId,
                privateKeyBase64,
                ttlDays
        );
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call() throws Exception;
    }
}
