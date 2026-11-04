package com.flashticket.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.identity.dto.RegisterRequest;
import com.flashticket.identity.service.RegistrationService;
import com.flashticket.common.exception.BizException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * M01 注册与邮箱验证集成测试：真实 MySQL 8.4 + Redis 7.4（Testcontainers）。
 *
 * <p>验证码经真实发送流程落 Redis，测试从 Redis 读取验证码完成注册，
 * 覆盖正常注册、唯一性、验证码错误/超次、发送限频、密码规则、邮箱规范化与并发唯一性兜底。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RegistrationIntegrationTest {

    private static final String PASSWORD = "SecurePass1234";
    private static final String CODE_KEY_PREFIX = "auth:verify:code:REGISTER:";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("flashticket");

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationService registrationService;

    @Test
    void registerShouldSucceedAndPersistUserProfileRole() throws Exception {
        sendCode("alice01@test.com");
        String code = fetchCode("alice01@test.com");

        register("alice01", "alice01@test.com", PASSWORD, code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("alice01"))
                .andExpect(jsonPath("$.data.email").value("alice01@test.com"));

        // 验收：MySQL 中正确产生用户、资料与默认 USER 角色关系
        assertThat(count("SELECT COUNT(*) FROM ft_user WHERE username = 'alice01'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM ft_user_profile p JOIN ft_user u ON p.user_id = u.id WHERE u.username = 'alice01'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM ft_user_role ur JOIN ft_user u ON ur.user_id = u.id JOIN ft_role r ON ur.role_id = r.id WHERE u.username = 'alice01' AND r.code = 'USER'")).isEqualTo(1);
    }

    @Test
    void registerWithWrongCodeShouldBeRejected() throws Exception {
        sendCode("bob01@test.com");
        register("bob01", "bob01@test.com", PASSWORD, "000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_VERIFY_CODE_INVALID"));
    }

    @Test
    void verifyCodeExceedingAttemptsShouldBeInvalidated() throws Exception {
        sendCode("carol01@test.com");
        for (int i = 0; i < 4; i++) {
            register("carol01", "carol01@test.com", PASSWORD, "000000")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("AUTH_VERIFY_CODE_INVALID"));
        }
        // 第 5 次错误触发超次
        register("carol01", "carol01@test.com", PASSWORD, "000000")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_VERIFY_CODE_ATTEMPTS_EXCEEDED"));
    }

    @Test
    void sendingTwiceWithin60SecondsShouldBeLimited() throws Exception {
        sendCode("dave01@test.com");
        // 立即再次发送 → 触发 60 秒发送限频
        String body = objectMapper.writeValueAsString(Map.of("email", "dave01@test.com", "purpose", "REGISTER"));
        mockMvc.perform(post("/api/v1/auth/verify-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_VERIFY_CODE_SENT_LIMIT"));
    }

    @Test
    void duplicateUsernameShouldBeRejectedBeforeCodeConsumed() throws Exception {
        // 注册第一个用户
        sendCode("dup1@test.com");
        register("dup_user", "dup1@test.com", PASSWORD, fetchCode("dup1@test.com"))
                .andExpect(status().isOk());

        // 用相同用户名 + 任意验证码注册（预检查先于核销，直接拒绝）
        register("dup_user", "dup2@test.com", PASSWORD, "000000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_USERNAME_TAKEN"));
    }

    @Test
    void duplicateEmailShouldBeRejected() throws Exception {
        sendCode("edup1@test.com");
        register("edup_user1", "edup1@test.com", PASSWORD, fetchCode("edup1@test.com"))
                .andExpect(status().isOk());

        register("edup_user2", "edup1@test.com", PASSWORD, "000000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_EMAIL_TAKEN"));
    }

    @Test
    void weakPasswordShouldBeRejectedByValidation() throws Exception {
        register("short01", "short01@test.com", "short", "000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"));
    }

    @Test
    void emailNormalizationShouldMatchSendAndRegister() throws Exception {
        // 发送时大小写混合，注册时小写 → 规范化后 key 一致，注册成功
        sendCode("Mixed@Example.COM");
        String code = fetchCode("mixed@example.com");

        register("mixed01", "mixed@example.com", PASSWORD, code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void concurrentRegisterSameUsernameShouldOnlyAllowOne() throws Exception {
        String username = "race_user";
        int n = 6;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            String email = "race" + i + "@test.com";
            sendCode(email);
            String code = fetchCode(email);
            futures.add(pool.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    RegisterRequest req = new RegisterRequest();
                    req.setUsername(username);
                    req.setEmail(email);
                    req.setPassword(PASSWORD);
                    req.setVerifyCode(code);
                    registrationService.register(req);
                    return true;
                } catch (BizException | InterruptedException ex) {
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();

        int success = 0;
        for (Future<Boolean> f : futures) {
            if (f.get()) {
                success++;
            }
        }
        pool.shutdown();

        assertThat(success).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM ft_user WHERE username = 'race_user'")).isEqualTo(1);
    }

    // ===== helpers =====

    private void sendCode(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "purpose", "REGISTER"));
        mockMvc.perform(post("/api/v1/auth/verify-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    private String fetchCode(String email) {
        String code = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email.toLowerCase(Locale.ROOT));
        assertThat(code).as("验证码应已写入 Redis").isNotNull();
        return code;
    }

    private org.springframework.test.web.servlet.ResultActions register(
            String username, String email, String password, String code) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", email,
                "password", password,
                "verifyCode", code));
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private int count(String sql) {
        Integer c = jdbcTemplate.queryForObject(sql, Integer.class);
        return c == null ? 0 : c;
    }
}
