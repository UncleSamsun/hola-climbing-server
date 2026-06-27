package com.holaclimbing.server.domain.user.oauth;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    private final ApplePrivateKeyParser privateKeyParser;
    private final Clock clock;

    @Autowired
    public AppleClientSecretGenerator(ApplePrivateKeyParser privateKeyParser) {
        this(privateKeyParser, Clock.systemUTC());
    }

    AppleClientSecretGenerator(ApplePrivateKeyParser privateKeyParser, Clock clock) {
        this.privateKeyParser = privateKeyParser;
        this.clock = clock;
    }

    public String generate(OAuthProperties.Provider provider) {
        validateProvider(provider);

        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        int ttlDays = provider.effectiveClientSecretTtlDays();
        validateTtlDays(ttlDays);
        Instant expiresAt = now.plus(ttlDays, ChronoUnit.DAYS);
        PrivateKey privateKey = privateKeyParser.parseBase64Pem(provider.privateKeyBase64());

        return Jwts.builder()
                .header().keyId(provider.keyId()).and()
                .issuer(provider.teamId())
                .subject(provider.clientId())
                .audience().add(APPLE_AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
    }

    private void validateProvider(OAuthProperties.Provider provider) {
        if (provider == null
                || !StringUtils.hasText(provider.clientId())
                || !StringUtils.hasText(provider.teamId())
                || !StringUtils.hasText(provider.keyId())
                || !StringUtils.hasText(provider.privateKeyBase64())) {
            throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
        }
    }

    private void validateTtlDays(int ttlDays) {
        if (ttlDays < 1 || ttlDays > 180) {
            throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
        }
    }
}
