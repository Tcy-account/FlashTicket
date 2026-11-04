package com.flashticket.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.flashticket.inventory.enums.AdmissionResult;
import com.flashticket.inventory.service.InventoryAdmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * M15-C Redis 高速库存准入集成测试：快照预减、售罄拒绝、快照缺失回退、对账校准。
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InventoryAdmissionIntegrationTest {

    private static final String KEY_PREFIX = "ft:inventory:admission:";

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
        // 启用 Redis 高速准入（模式3）
        registry.add("flashticket.inventory.redis-admission.enabled", () -> "true");
    }

    @Autowired
    private InventoryAdmissionService admissionService;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /* ================= 测试用例 ================= */

    @Test
    void tryAdmitFallsBackWithoutSnapshot() {
        // 快照未建立 → 回退 MySQL（FALLBACK，不阻断交易）
        assertThat(admissionService.tryAdmit(999L, 1)).isEqualTo(AdmissionResult.FALLBACK);
    }

    @Test
    void tryAdmitAdmitsAndDecrementsSnapshot() {
        insertInventory(100L, 10);
        admissionService.reconcile();

        // 首次预减成功，快照 available 10 → 9
        assertThat(admissionService.tryAdmit(100L, 1)).isEqualTo(AdmissionResult.ADMIT);
        assertThat(redisAvailable(100L)).isEqualTo("9");
    }

    @Test
    void tryAdmitRejectsWhenSnapshotSoldOut() {
        insertInventory(200L, 2);
        admissionService.reconcile();

        // 预减 2 张成功，快照归零；再预减 1 张 → 售罄拒绝
        assertThat(admissionService.tryAdmit(200L, 2)).isEqualTo(AdmissionResult.ADMIT);
        assertThat(admissionService.tryAdmit(200L, 1)).isEqualTo(AdmissionResult.SOLD_OUT);
    }

    @Test
    void reconcileCalibratesSnapshotFromMysql() {
        insertInventory(300L, 10);
        admissionService.reconcile();
        assertThat(redisAvailable(300L)).isEqualTo("10");

        // MySQL 权威扣减后（available 减 + reserved 增，保持守恒），对账刷新快照
        jdbcTemplate.update("UPDATE ft_inventory SET available = 3, reserved = 7 WHERE tier_id = 300");
        admissionService.reconcile();
        assertThat(redisAvailable(300L)).isEqualTo("3");
    }

    /* ================= 辅助方法 ================= */

    private void insertInventory(long tierId, int available) {
        jdbcTemplate.update(
                "INSERT INTO ft_inventory (tier_id, total, available, reserved, sold, version, generation) " +
                        "VALUES (?, ?, ?, 0, 0, 0, 1)", tierId, available, available);
    }

    private String redisAvailable(long tierId) {
        return redis.opsForValue().get(KEY_PREFIX + tierId);
    }
}
