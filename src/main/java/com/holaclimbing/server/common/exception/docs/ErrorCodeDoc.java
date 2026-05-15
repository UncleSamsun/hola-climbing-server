package com.holaclimbing.server.common.exception.docs;

import com.holaclimbing.server.common.exception.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 코드 카탈로그 항목")
public record ErrorCodeDoc(
        @Schema(example = "V001") String code,
        @Schema(example = "404") int status,
        @Schema(example = "비디오") String domain,
        @Schema(example = "영상을 찾을 수 없습니다.") String defaultMessage
) {
    public static ErrorCodeDoc from(ErrorCode ec) {
        return new ErrorCodeDoc(
                ec.getCode(),
                ec.getStatus().value(),
                resolveDomain(ec.getCode()),
                ec.getDefaultMessage()
        );
    }

    private static String resolveDomain(String code) {
        return switch (code.charAt(0)) {
            case 'C' -> "공통";
            case 'U' -> "회원 (F-01)";
            case 'V' -> "비디오 (F-02)";
            case 'G' -> "암장 (F-04)";
            case 'N' -> "알림 (F-08)";
            case 'R' -> "신고 (F-09)";
            case 'S' -> "인프라";
            default  -> "기타";
        };
    }
}