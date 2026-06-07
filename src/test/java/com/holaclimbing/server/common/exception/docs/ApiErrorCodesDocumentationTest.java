package com.holaclimbing.server.common.exception.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaclimbing.server.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link ApiErrorCodes} → Swagger 문서화가 실제 OpenAPI 스펙에 반영되는지 검증.
 * {@code /v1/api-docs}(springdoc.api-docs.path 커스텀)를 받아 status별 응답·example을 확인한다.
 */
@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:3000")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ApiErrorCodesDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("@ApiErrorCodes가 OpenAPI 스펙의 응답·example로 반영된다")
    void apiErrorCodesAppearInOpenApiSpec() throws Exception {
        String json = mockMvc.perform(get("/v1/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode root = objectMapper.readTree(json);
        JsonNode loginResponses = root
                .path("paths")
                .path("/api/auth/login")
                .path("post")
                .path("responses");

        // login은 USER_NOT_FOUND(404)·PASSWORD_MISMATCH(401)·EMAIL_NOT_VERIFIED/USER_SUSPENDED(403)을 낸다.
        assertThat(loginResponses.has("401")).isTrue();
        assertThat(loginResponses.has("403")).isTrue();
        assertThat(loginResponses.has("404")).isTrue();

        // 401 응답에 PASSWORD_MISMATCH(U003) example이 코드값과 함께 박혀 있어야 한다.
        JsonNode u003 = loginResponses
                .path("401")
                .path("content")
                .path("application/json")
                .path("examples")
                .path("U003");
        assertThat(u003.isMissingNode()).isFalse();
        assertThat(u003.path("value").path("code").asText()).isEqualTo("U003");
        assertThat(u003.path("value").path("isSuccess").asBoolean()).isFalse();

        JsonNode u012 = loginResponses
                .path("403")
                .path("content")
                .path("application/json")
                .path("examples")
                .path("U012");
        assertThat(u012.isMissingNode()).isFalse();
        assertThat(u012.path("value").path("code").asText()).isEqualTo("U012");
    }
}
