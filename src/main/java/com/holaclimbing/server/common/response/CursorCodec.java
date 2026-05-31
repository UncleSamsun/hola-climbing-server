package com.holaclimbing.server.common.response;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 커서 인코딩 유틸. 내부적으로는 마지막 항목의 id(Long)지만,
 * 클라이언트에는 base64로 감싼 불투명(opaque) 문자열로 노출해 내부 구조 의존을 막는다.
 */
public final class CursorCodec {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorCodec() {
    }

    /** id를 불투명 커서 문자열로 인코딩. */
    public static String encode(long id) {
        return ENCODER.encodeToString(Long.toString(id).getBytes(StandardCharsets.UTF_8));
    }

    /** 커서 문자열을 id로 디코딩. null/blank면 null(첫 페이지). 형식 오류면 400. */
    public static Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(new String(DECODER.decode(cursor), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 커서입니다.");
        }
    }
}
