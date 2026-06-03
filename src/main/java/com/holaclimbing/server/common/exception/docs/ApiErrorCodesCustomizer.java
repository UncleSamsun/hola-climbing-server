package com.holaclimbing.server.common.exception.docs;

import com.holaclimbing.server.common.exception.error.ErrorCode;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link ApiErrorCodes}가 붙은 컨트롤러 메서드의 에러 코드를
 * Swagger 오퍼레이션 응답에 HTTP status별로 묶어 문서화한다.
 *
 * <p>같은 status를 공유하는 여러 ErrorCode는 하나의 응답 항목 아래
 * 다중 example로 노출되어 Swagger UI에서 드롭다운으로 비교할 수 있다.
 * 메시지·상태는 모두 {@link ErrorCode} enum에서 끌어오므로 문서가 코드와 항상 동기화된다.
 */
@Configuration
public class ApiErrorCodesCustomizer {

    private static final String JSON = "application/json";

    @Bean
    public OperationCustomizer apiErrorCodesOperationCustomizer() {
        return (operation, handlerMethod) -> {
            ApiErrorCodes annotation = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
            if (annotation == null || annotation.value().length == 0) {
                return operation;
            }

            ApiResponses responses = operation.getResponses();

            Map<Integer, List<ErrorCode>> byStatus = Arrays.stream(annotation.value())
                    .collect(Collectors.groupingBy(
                            ec -> ec.getStatus().value(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            byStatus.forEach((status, codes) -> {
                MediaType mediaType = new MediaType();
                for (ErrorCode ec : codes) {
                    mediaType.addExamples(ec.getCode(), toExample(ec));
                }

                String description = codes.stream()
                        .map(ec -> "`" + ec.getCode() + "` " + ec.getDefaultMessage())
                        .collect(Collectors.joining("\n"));

                Content content = new Content().addMediaType(JSON, mediaType);
                responses.addApiResponse(
                        String.valueOf(status),
                        new ApiResponse().description(description).content(content));
            });

            return operation;
        };
    }

    private Example toExample(ErrorCode ec) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("isSuccess", false);
        body.put("code", ec.getCode());
        body.put("message", ec.getDefaultMessage());
        body.put("timestamp", Instant.parse("2026-06-04T00:00:00Z").toString());

        return new Example()
                .summary(ec.getCode() + " - " + ec.getDefaultMessage())
                .value(body);
    }
}
