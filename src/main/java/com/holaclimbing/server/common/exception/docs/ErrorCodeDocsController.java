package com.holaclimbing.server.common.exception.docs;

import com.holaclimbing.server.common.exception.error.ErrorCode;
import com.holaclimbing.server.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/docs")
@Tag(name = "ErrorCode Catalog", description = "전체 에러 코드 목록 (개발 참고용)")
public class ErrorCodeDocsController {

    @Operation(summary = "전체 에러 코드 목록", description = "클라이언트 분기 처리용 참고 문서")
    @GetMapping("/error-codes")
    public ApiResponse<List<ErrorCodeDoc>> listErrorCodes() {
        List<ErrorCodeDoc> codes = Arrays.stream(ErrorCode.values())
                .map(ErrorCodeDoc::from)
                .toList();
        return ApiResponse.success(codes);
    }
}