package com.holaclimbing.server.common.exception.docs;

import com.holaclimbing.server.common.exception.error.ErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 해당 API 오퍼레이션이 응답할 수 있는 {@link ErrorCode}를 Swagger 문서에 노출한다.
 * {@code ApiErrorCodesCustomizer}가 이 어노테이션을 읽어 HTTP status별로
 * 응답 예시(ApiResponse error 형태)를 자동 생성한다.
 *
 * <pre>{@code
 * @ApiErrorCodes({USER_NOT_FOUND, PASSWORD_MISMATCH})
 * @PostMapping("/login")
 * public ... login(...) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {
    ErrorCode[] value();
}
