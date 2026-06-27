package com.holaclimbing.server.domain.user.oauth;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
public class ApplePrivateKeyParser {

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    public PrivateKey parseBase64Pem(String privateKeyBase64) {
        try {
            byte[] pemBytes = Base64.getDecoder().decode(privateKeyBase64);
            String pem = new String(pemBytes, StandardCharsets.UTF_8);
            String pkcs8 = pem
                    .replace(BEGIN_PRIVATE_KEY, "")
                    .replace(END_PRIVATE_KEY, "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pkcs8);

            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
        }
    }
}
