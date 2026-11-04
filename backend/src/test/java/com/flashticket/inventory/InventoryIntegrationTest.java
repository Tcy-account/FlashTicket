package com.flashticket.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.entity.User;
import com.flashticket.inventory.entity.Reservation;
import com.flashticket.inventory.enums.ReservationStatus;
import com.flashticket.inventory.service.InventoryService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * M07 MySQL 库存总账与用户限购集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：发布初始化库存、reserve/confirm/release 全生命周期守恒、并发抢票不超卖、
 * 重复 requestId 幂等、重复 confirm/release 不重复变更、限购超限拒绝、非法状态转换、
 * 库存不足拒绝、后台库存查询权限。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class InventoryIntegrationTest {

    private static final String PASSWORD = "ZewruqYvgn2099";
    private static final String REGISTER_CODE_KEY = "auth:verify:code:REGISTER:";

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private RbacDomainService rbacDomainService;

    @Autowired
    private InventoryService inventoryService;

    // ===== 初始化 =====

    @Test
    void publishInitializesInventory() throws Exception {
        String tierId = publishWithTier("init", 100);

        mockMvc.perform(get("/api/v1/admin/inventory/{tierId}", tierId)
                        .header("Authorization", "Bearer " + opTokenOf("init")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100))
                .andExpect(jsonPath("$.data.available").value(100))
                .andExpect(jsonPath("$.data.reserved").value(0))
                .andExpect(jsonPath("$.data.sold").value(0));

        // 初始化流水存在
        Integer ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_inventory_ledger WHERE tier_id = ? AND operation = 'INIT'",
                Integer.class, Long.valueOf(tierId));
        assertThat(ledgerCount).isEqualTo(1);
    }

    // ===== 预占/确认/释放全生命周期 =====

    @Test
    void reserveConfirmReleaseConservesInventory() throws Exception {
        Long tierId = Long.valueOf(publishWithTier("life", 100));

        inventoryService.reserve("life-a", tierId, 1001L, 2, expire());
        inventoryService.reserve("life-b", tierId, 1002L, 2, expire());
        assertStock(tierId, 100, 96, 4, 0);

        inventoryService.confirm("life-a");
        assertStock(tierId, 100, 96, 2, 2);

        inventoryService.release("life-b");
        assertStock(tierId, 100, 98, 0, 2);
    }

    // ===== 并发不超卖 =====

    @Test
    void concurrentReserveDoesNotOversell() throws Exception {
        Long tierId = Long.valueOf(publishWithTier("conc", 10));
        int total = 10;
        int threads = 20;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    inventoryService.reserve("conc-req-" + idx, tierId, 5000L + idx, 1, expire());
                    success.incrementAndGet();
                } catch (BizException e) {
                    if (e.getErrorCode() == ErrorCode.INVENTORY_SOLD_OUT) {
                        soldOut.incrementAndGet();
                    } else {
                        throw new RuntimeException(e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS); // 若线程内抛异常，在此暴露
        }

        assertThat(success.get()).isEqualTo(total);
        assertThat(soldOut.get()).isEqualTo(threads - total);
        assertStock(tierId, total, 0, total, 0);
    }

    // ===== 幂等 =====

    @Test
    void duplicateRequestIdReservesOnlyOnce() throws Exception {
        Long tierId = Long.valueOf(publishWithTier("dup", 100));

        Reservation first = inventoryService.reserve("dup-req", tierId, 1001L, 2, expire());
        Reservation second = inventoryService.reserve("dup-req", tierId, 1001L, 2, expire());

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.RESERVED.name());
        assertStock(tierId, 100, 98, 2, 0);
    }

    @Test
    void confirmAndReleaseAreIdempotent() throws Exception {
        Long tierId = Long.valueOf(publishWithTier("idem", 100));

        inventoryService.reserve("idem-confirm", tierId, 1001L, 2, expire());
        inventoryService.reserve("idem-release", tierId, 1002L, 2, expire());
        assertStock(tierId, 100, 96, 4, 0);

        // 重复确认：sold 只增一次
        inventoryService.confirm("idem-confirm");
        inventoryService.confirm("idem-confirm");
        assertStock(tierId, 100, 96, 2, 2);

        // 重复释放：available 只增一次
        inventoryService.release("idem-release");
        inventoryService.release("idem-release");
        assertStock(tierId, 100, 98, 0, 2);
    }

    // ===== 限购 =====

    @Test
    void limitExceededRejected() throws Exception {
        Long tierId = Long.valueOf(publishWithTier("lim", 100));

        inventoryService.reserve("lim-1", tierId, 1001L, 1, expire());
        inventoryService.reserve("lim-2", tierId, 1001L, 1, expire());

        // perUserLimit=2，第三次超限
        assertThatThrownBy(() -> inventoryService.reserve("lim-3", tierId, 1001L, 1, expire()))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_LIMIT_EXCEEDED);

        // 超限拒绝后库存未变
        assertStock(tierId, 100, 98, 2, 0);
    }

    // ===== 非法状态转换与库存不足 =====

    @Test
    void releasedCannotBeConfirmedAndConfirmedCannotBeReleased() throws Exception {
        Long tierId = Long.valueOf(publishWithTier("state", 100));

        inventoryService.reserve("state-confirm", tierId, 1001L, 2, expire());
        inventoryService.confirm("state-confirm");

        // 已售不可普通释放
        assertThatThrownBy(() -> inventoryService.release("state-confirm"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_RESERVATION_ILLEGAL_TRANSITION);

        inventoryService.reserve("state-release", tierId, 1002L, 1, expire());
        inventoryService.release("state-release");

        // 已释放不可再确认
        assertThatThrownBy(() -> inventoryService.confirm("state-release"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_RESERVATION_ILLEGAL_TRANSITION);
    }

    @Test
    void soldOutRejected() throws Exception {
        Long tierId = Long.valueOf(publishWithTier("soldout", 5));

        assertThatThrownBy(() -> inventoryService.reserve("soldout-req", tierId, 1001L, 6, expire()))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_SOLD_OUT);
        assertStock(tierId, 5, 5, 0, 0);
    }

    // ===== 后台查询权限 =====

    @Test
    void adminWithoutInventoryPermissionForbidden() throws Exception {
        String tierId = publishWithTier("perm", 100);
        makeAdmin("permadmin");

        mockMvc.perform(get("/api/v1/admin/inventory/{tierId}", tierId)
                        .header("Authorization", "Bearer " + accessToken(
                                adminLogin("permadmin", PASSWORD).andReturn())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ===== helpers =====

    private String publishWithTier(String prefix, int quantity) throws Exception {
        makeOperator(prefix + "op");
        makeSuperAdmin(prefix + "sa");
        String opToken = accessToken(adminLogin(prefix + "op", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin(prefix + "sa", PASSWORD).andReturn());
        String eventId = createFullEvent(opToken, "库存测试活动" + prefix);
        String venueId = createVenue(opToken, "库存场馆" + prefix, "北京", 5000);
        String sessionId = createSession(opToken, eventId, venueId, "晚场",
                "2027-05-01T19:00:00", "2027-05-01T21:00:00", 2000);
        String tierId = createTier(opToken, sessionId, "普通票", 100L, quantity);
        configureSalePlan(opToken, tierId, "2027-04-01T10:00:00", "2027-05-01T18:00:00");

        postEvent(opToken, "/api/v1/admin/events/{id}/submit-review", eventId, null)
                .andExpect(status().isOk());
        postEvent(saToken, "/api/v1/admin/events/{id}/review", eventId, Map.of("approve", true))
                .andExpect(status().isOk());
        postEvent(saToken, "/api/v1/admin/events/{id}/publish", eventId, null)
                .andExpect(status().isOk());
        return tierId;
    }

    private ResultActions postEvent(String token, String path, String eventId, Map<String, Object> body) {
        try {
            if (body == null) {
                return mockMvc.perform(post(path, eventId)
                        .header("Authorization", "Bearer " + token));
            }
            return mockMvc.perform(post(path, eventId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String opTokenOf(String prefix) throws Exception {
        return accessToken(adminLogin(prefix + "op", PASSWORD).andReturn());
    }

    private LocalDateTime expire() {
        return LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15);
    }

    private void assertStock(Long tierId, int total, int available, int reserved, int sold) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT total, available, reserved, sold FROM ft_inventory WHERE tier_id = ?", tierId);
        assertThat(((Number) row.get("total")).intValue()).as("total").isEqualTo(total);
        assertThat(((Number) row.get("available")).intValue()).as("available").isEqualTo(available);
        assertThat(((Number) row.get("reserved")).intValue()).as("reserved").isEqualTo(reserved);
        assertThat(((Number) row.get("sold")).intValue()).as("sold").isEqualTo(sold);
    }

    // ===== 账号与演出域 helper（复用 M06 测试风格） =====

    private ResultActions registerUser(String username, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "purpose", "REGISTER"))))
                .andExpect(status().isOk());
        String code = redisTemplate.opsForValue().get(REGISTER_CODE_KEY + email.toLowerCase(Locale.ROOT));
        assertThat(code).isNotNull();
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username, "email", email, "password", PASSWORD, "verifyCode", code))));
    }

    private void makeOperator(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        rbacDomainService.assignUserRoles(user.getId(),
                List.of(rbacDomainService.findRoleByCode("USER").getId(),
                        rbacDomainService.findRoleByCode("OPERATOR").getId()));
    }

    private void makeSuperAdmin(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        rbacDomainService.assignUserRoles(user.getId(),
                List.of(rbacDomainService.findRoleByCode("USER").getId(),
                        rbacDomainService.findRoleByCode("SUPER_ADMIN").getId()));
    }

    private void makeAdmin(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        rbacDomainService.assignUserRoles(user.getId(),
                List.of(rbacDomainService.findRoleByCode("USER").getId(),
                        rbacDomainService.findRoleByCode("ADMIN").getId()));
    }

    private ResultActions adminLogin(String account, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("account", account, "password", password))));
    }

    private String accessToken(MvcResult loginResult) {
        try {
            return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                    .get("data").get("accessToken").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createVenue(String token, String name, String city, int maxCapacity) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "countryCode", "CN", "city", city,
                                "address", city + "某路 1 号", "timezone", "Asia/Shanghai",
                                "maxCapacity", maxCapacity))))
                .andExpect(status().isOk())
                .andReturn();
        return json(created).get("id").asText();
    }

    private String createFullEvent(String token, String title) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("categoryId", "1");
        body.put("title", title);
        body.put("version", 0);
        body.put("coverUrl", "https://img.example.com/cover.jpg");
        body.put("detailContent", "演出详细介绍");
        body.put("organizerName", "主办方文化传媒");
        body.put("attendanceNotice", "请提前 60 分钟入场");
        MvcResult created = mockMvc.perform(post("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return json(created).get("id").asText();
    }

    private String createSession(String token, String eventId, String venueId, String name,
                                 String startAt, String endAt, int saleCapacity) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/admin/events/{eventId}/sessions", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "venueId", venueId, "name", name,
                                "startAt", startAt, "endAt", endAt, "saleCapacity", saleCapacity))))
                .andExpect(status().isOk())
                .andReturn();
        return json(created).get("id").asText();
    }

    private String createTier(String token, String sessionId, String name,
                              long priceCent, int quantity) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("unitPriceCent", priceCent);
        body.put("initialQuantity", quantity);
        body.put("minPurchase", 1);
        body.put("maxPurchase", 6);
        body.put("perUserLimit", 2);
        MvcResult created = mockMvc.perform(post("/api/v1/admin/sessions/{sessionId}/tiers", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return json(created).get("id").asText();
    }

    private void configureSalePlan(String token, String tierId, String startAt, String endAt) throws Exception {
        mockMvc.perform(put("/api/v1/admin/tiers/{id}/sale-plan", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "saleStartAt", startAt, "saleEndAt", endAt))))
                .andExpect(status().isOk());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
