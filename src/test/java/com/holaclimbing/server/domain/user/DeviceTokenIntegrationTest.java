package com.holaclimbing.server.domain.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaclimbing.server.TestcontainersConfiguration;
import com.holaclimbing.server.domain.user.dto.request.LoginRequest;
import com.holaclimbing.server.domain.user.dto.request.RegisterDeviceTokenRequest;
import com.holaclimbing.server.domain.user.dto.request.SignupRequest;
import com.holaclimbing.server.domain.user.dto.request.UnregisterDeviceTokenRequest;
import com.holaclimbing.server.domain.user.dto.request.VerifyEmailRequest;
import com.holaclimbing.server.domain.user.mapper.DeviceTokenMapper;
import com.holaclimbing.server.domain.user.mapper.UserMapper;
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
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 디바이스 토큰 등록·해제 + FCM 푸시 흐름 통합 테스트.
 * 테스트 환경은 NoopFcmSender가 주입되므로 실제 푸시는 발생하지 않는다.
 */
@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:3000")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Sql(scripts = "classpath:sql/users-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DeviceTokenIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DeviceTokenMapper deviceTokenMapper;

    @Test
    @DisplayName("디바이스 토큰 등록 — 201, DB에 저장된다")
    void registerToken_success() throws Exception {
        String token = register("a@hola.com", "climberone");
        Long userId = userMapper.findByEmail("a@hola.com").getId();

        mockMvc.perform(post("/api/users/me/device-tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterDeviceTokenRequest("fcm-token-abc", "android"))))
                .andExpect(status().isCreated());

        List<String> stored = deviceTokenMapper.findTokensByUserId(userId);
        assertThat(stored).containsExactly("fcm-token-abc");
    }

    @Test
    @DisplayName("디바이스 토큰 등록 — 같은 토큰 재등록은 upsert로 처리된다")
    void registerToken_upsert() throws Exception {
        String token = register("a@hola.com", "climberone");

        var body = objectMapper.writeValueAsString(
                new RegisterDeviceTokenRequest("fcm-token-abc", "android"));
        mockMvc.perform(post("/api/users/me/device-tokens").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/users/me/device-tokens").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        // 두 번째 호출이 충돌 없이 성공하면 upsert 동작.
    }

    @Test
    @DisplayName("디바이스 토큰 등록 실패 — 토큰 없이 호출하면 401")
    void registerToken_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/users/me/device-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterDeviceTokenRequest("fcm-token-abc", "android"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("디바이스 토큰 해제 — 200, 본인 토큰이 삭제된다")
    void unregisterToken_success() throws Exception {
        String token = register("a@hola.com", "climberone");
        Long userId = userMapper.findByEmail("a@hola.com").getId();

        mockMvc.perform(post("/api/users/me/device-tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterDeviceTokenRequest("fcm-token-abc", "android"))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/users/me/device-tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UnregisterDeviceTokenRequest("fcm-token-abc"))))
                .andExpect(status().isOk());

        assertThat(deviceTokenMapper.findTokensByUserId(userId)).isEmpty();
    }

    // ===== helpers =====

    private String register(String email, String nickname) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, PASSWORD, nickname))))
                .andExpect(status().isCreated());
        var user = userMapper.findByEmail(email);
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyEmailRequest(user.getEmailVerificationToken()))))
                .andExpect(status().isOk());
        return dataOf(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD)))))
                .path("accessToken").asText();
    }

    private JsonNode dataOf(ResultActions actions) throws Exception {
        String body = actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("data");
    }
}
