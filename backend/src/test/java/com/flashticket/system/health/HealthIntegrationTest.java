package com.flashticket.system.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * M00 基础集成测试：真实 MySQL 8.4 + Redis 7.4（Testcontainers）。
 *
 * <p>覆盖契约三项验收：健康检查成功态、就绪探针依赖连通、业务异常统一映射。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class HealthIntegrationTest {

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

    @Test
    void livenessShouldReturnSuccessEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void readinessShouldPassWithRealDependencies() throws Exception {
        mockMvc.perform(get("/api/v1/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.db").value("UP"))
                .andExpect(jsonPath("$.data.redis").value("UP"));
    }

    @Test
    @WithMockUser
    void bizExceptionShouldMapToErrorCodeAndHttpStatus() throws Exception {
        mockMvc.perform(get("/test/biz-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONFLICT.getCode()))
                .andExpect(jsonPath("$.message").value("模拟业务冲突"));
    }
}
