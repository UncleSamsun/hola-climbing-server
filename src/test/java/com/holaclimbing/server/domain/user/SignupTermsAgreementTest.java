package com.holaclimbing.server.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaclimbing.server.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원가입 termsAgreed contract 회귀 방지 테스트.
 * DTO 자동 직렬화가 아닌 raw JSON으로 보내서 실제 프론트 페이로드와 동일한 형태로 검증한다.
 *
 * <p>이 테스트가 잡고자 하는 회귀:
 * (1) JSON 키 네이밍 (camelCase) — `termsAgreed`, `termId`
 * (2) `termId`가 Long으로 deserialize되는지 (문자열 슬러그 X)
 * (3) 필수 약관에 `agreed:true`를 주면 201, 빠뜨리면 400 U010</p>
 */
@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:3000")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:sql/users-schema.sql",
        "classpath:sql/terms-data.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SignupTermsAgreementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 — 프론트가 보낸 그대로의 camelCase + 숫자 termId로 201")
    void signup_withCamelCaseAndNumericTermIds_succeeds() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "password": "Password1234!",
                  "nickname": "테스트닉네임",
                  "termsAgreed": [
                    { "termId": 1, "agreed": true },
                    { "termId": 2, "agreed": true },
                    { "termId": 3, "agreed": true }
                  ]
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("테스트닉네임"))
                .andExpect(jsonPath("$.data.emailVerified").value(false));
    }

    @Test
    @DisplayName("회원가입 — 마케팅(선택)만 false여도 201")
    void signup_optionalTermDeclined_succeeds() throws Exception {
        String body = """
                {
                  "email": "test2@example.com",
                  "password": "Password1234!",
                  "nickname": "테스트투",
                  "termsAgreed": [
                    { "termId": 1, "agreed": true },
                    { "termId": 2, "agreed": true },
                    { "termId": 3, "agreed": false }
                  ]
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("회원가입 실패 — 필수 약관(privacy) 미동의는 400 U010")
    void signup_missingRequiredTerm_returnsU010() throws Exception {
        String body = """
                {
                  "email": "test3@example.com",
                  "password": "Password1234!",
                  "nickname": "테스트쓰리",
                  "termsAgreed": [
                    { "termId": 1, "agreed": true },
                    { "termId": 2, "agreed": false }
                  ]
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U010"));
    }
}
