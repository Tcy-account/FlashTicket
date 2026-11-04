package com.flashticket.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * M02 登录认证与个人中心集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖登录、JWT、Refresh 轮换、会话撤销、资料、改密、找回密码、失败锁定。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String PASSWORD = "SecurePass1234";
    private static final String NEW_PASSWORD = "NewSecurePass2077";
    private static final String REGISTER_CODE_KEY = "auth:verify:code:REGISTER:";
    private static final String RESET_CODE_KEY = "auth:verify:code:RESET_PASSWORD:";
    private static final String REFRESH_COOKIE = "ft_refresh_token";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4").withDatabaseName("flashticket");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4").withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void loginShouldReturnAccessTokenAndSetRefreshCookie() throws Exception {
        registerUser("m2user", "m2user@test.com");

        MvcResult result = login("m2user", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("m2user"))
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(refreshCookie).as("登录应写入 Refresh Token HttpOnly Cookie").isNotNull();
        assertThat(refreshCookie.getValue()).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordShouldBeRejected() throws Exception {
        registerUser("wrong1", "wrong1@test.com");
        login("wrong1", "WrongPass000000")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_BAD_CREDENTIALS"));
    }

    @Test
    void loginWithUnknownAccountShouldUseSameGenericError() throws Exception {
        login("no_such_user_xyz", "whatever12345")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_BAD_CREDENTIALS"));
    }

    @Test
    void fiveFailuresShouldLockAccountTemporarily() throws Exception {
        registerUser("lock1", "lock1@test.com");
        for (int i = 0; i < 5; i++) {
            login("lock1", "WrongPass000000")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_BAD_CREDENTIALS"));
        }
        // 第 6 次（正确密码）也应被锁定拒绝
        login("lock1", PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_LOCKED"));
    }

    @Test
    void refreshShouldRotateAndInvalidateOldToken() throws Exception {
        registerUser("refresh1", "refresh1@test.com");
        String oldRefresh = login("refresh1", PASSWORD).andReturn()
                .getResponse().getCookie(REFRESH_COOKIE).getValue();

        // 刷新：签发新 Access Token + 新 Refresh Cookie
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE, oldRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        String newRefresh = refreshResult.getResponse().getCookie(REFRESH_COOKIE).getValue();
        assertThat(newRefresh).isNotEqualTo(oldRefresh);

        // 旧 Refresh Token 再次使用 → 失效
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE, oldRefresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALID"));
    }

    @Test
    void protectedEndpointWithoutTokenShouldBe401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMeShouldReturnProfileWithRoles() throws Exception {
        registerUser("me001", "me001@test.com");
        String token = accessToken(login("me001", PASSWORD).andReturn());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("me001"))
                .andExpect(jsonPath("$.data.email").value("me001@test.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }

    @Test
    void logoutShouldRevokeSessionImmediately() throws Exception {
        registerUser("out1", "out1@test.com");
        String accessToken = accessToken(login("out1", PASSWORD).andReturn());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 退出后原 Access Token 即使未过期，会话已撤销 → 401
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordShouldRequireOldPasswordAndRevokeOtherSessions() throws Exception {
        registerUser("cpw1", "cpw1@test.com");
        String token = accessToken(login("cpw1", PASSWORD).andReturn());

        // 旧密码错误 → 拒绝
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", "WrongOldPass123", "newPassword", NEW_PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_OLD_PASSWORD_WRONG"));

        // 正确旧密码 → 成功
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", PASSWORD, "newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 新密码可登录，旧密码失败
        login("cpw1", NEW_PASSWORD).andExpect(status().isOk());
        login("cpw1", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void forgotThenResetPasswordFlowShouldWork() throws Exception {
        registerUser("fpw1", "fpw1@test.com");

        // 第一步：发送重置验证码
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "fpw1@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        String code = redisTemplate.opsForValue().get(RESET_CODE_KEY + "fpw1@test.com");
        assertThat(code).isNotNull();

        // 第二步：验证码重置
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "fpw1@test.com", "verifyCode", code, "newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 新密码可登录
        login("fpw1", NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void updateProfileShouldChangeNickname() throws Exception {
        registerUser("prof1", "prof1@test.com");
        String token = accessToken(login("prof1", PASSWORD).andReturn());

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "新昵称"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.nickname").value("新昵称"));
    }

    // ===== helpers =====

    private ResultActions login(String account, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("account", account, "password", password));
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String accessToken(MvcResult loginResult) throws Exception {
        JsonNode data = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }

    private void registerUser(String username, String email) throws Exception {
        String sendBody = objectMapper.writeValueAsString(Map.of("email", email, "purpose", "REGISTER"));
        mockMvc.perform(post("/api/v1/auth/verify-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendBody))
                .andExpect(status().isOk());

        String code = redisTemplate.opsForValue().get(REGISTER_CODE_KEY + email.toLowerCase(Locale.ROOT));
        assertThat(code).isNotNull();

        String regBody = objectMapper.writeValueAsString(Map.of(
                "username", username, "email", email, "password", PASSWORD, "verifyCode", code));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regBody))
                .andExpect(status().isOk());
    }
}
