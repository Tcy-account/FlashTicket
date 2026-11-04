package com.flashticket.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.entity.User;
import com.flashticket.purchase.service.PurchaseRequestProcessor;
import com.flashticket.order.service.OrderCloseService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * M09 订单取消与支付超时关单集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：用户取消释放库存与限购、重复取消幂等不重复释放、越权取消、已支付拒绝取消、
 * 订单不存在、支付超时关单释放库存与限购、超时关单幂等、操作流水记录与归属校验。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OrderCloseIntegrationTest {

    private static final String PASSWORD = "Test@123456789";
    private static final String REGISTER_CODE_KEY = "auth:verify:code:REGISTER:";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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
    private PurchaseRequestProcessor processor;

    @Autowired
    private OrderCloseService orderCloseService;

    // ===== 用户取消 =====

    @Test
    void cancelOrderReleasesInventoryAndLimit() throws Exception {
        String tierId = publishSellingTier("c1", 100);
        registerUser("c1buyer", "c1buyer@test.com").andExpect(status().isOk());
        Long userId = userIdOf("c1buyer");
        String token = userToken("c1buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-c1-" + System.nanoTime(), tierId, 2);

        // 预占后：available 减少 2，reserved 增加 2；限购占用 2
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);
        assertLimit(userId, Long.valueOf(tierId), 2, 0);

        // 取消订单
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closeReason").value("USER_CANCEL"));

        // 取消后：库存与限购完全恢复
        assertStock(Long.valueOf(tierId), 100, 100, 0, 0);
        assertLimit(userId, Long.valueOf(tierId), 0, 0);
    }

    @Test
    void cancelOrderIsIdempotent() throws Exception {
        String tierId = publishSellingTier("c2", 100);
        registerUser("c2buyer", "c2buyer@test.com").andExpect(status().isOk());
        Long userId = userIdOf("c2buyer");
        String token = userToken("c2buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-c2-" + System.nanoTime(), tierId, 2);

        // 第一次取消
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
        // 第二次取消：幂等返回 CLOSED，库存不重复释放
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        assertStock(Long.valueOf(tierId), 100, 100, 0, 0);
        assertLimit(userId, Long.valueOf(tierId), 0, 0);
    }

    @Test
    void cancelOrderRejectsCrossUserAccess() throws Exception {
        String tierId = publishSellingTier("c3", 100);
        registerUser("c3owner", "c3owner@test.com").andExpect(status().isOk());
        registerUser("c3other", "c3other@test.com").andExpect(status().isOk());
        String ownerToken = userToken("c3owner", PASSWORD);
        String otherToken = userToken("c3other", PASSWORD);

        String orderId = checkoutOrder(ownerToken, "req-c3-" + System.nanoTime(), tierId, 1);

        // 他人取消 → 403 越权，订单仍待支付，库存不变
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_OWNED"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status FROM ft_order WHERE id = ?", Long.valueOf(orderId));
        assertThat(row.get("status")).isEqualTo("PENDING_PAYMENT");
        assertStock(Long.valueOf(tierId), 100, 99, 1, 0);
    }

    @Test
    void cancelOrderRejectsPaidOrder() throws Exception {
        String tierId = publishSellingTier("c4", 100);
        registerUser("c4buyer", "c4buyer@test.com").andExpect(status().isOk());
        String token = userToken("c4buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-c4-" + System.nanoTime(), tierId, 1);

        // 模拟已支付（M09 无支付模块，直接置状态）
        jdbcTemplate.update("UPDATE ft_order SET status = 'PAID', paid_at = NOW(3) WHERE id = ?",
                Long.valueOf(orderId));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELLABLE"));
    }

    @Test
    void cancelNonExistentOrderReturnsNotFound() throws Exception {
        registerUser("c5buyer", "c5buyer@test.com").andExpect(status().isOk());
        String token = userToken("c5buyer", PASSWORD);

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", "999999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    // ===== 支付超时关单 =====

    @Test
    void timeoutCloseReleasesInventoryAndLimit() throws Exception {
        String tierId = publishSellingTier("c6", 100);
        registerUser("c6buyer", "c6buyer@test.com").andExpect(status().isOk());
        Long userId = userIdOf("c6buyer");
        String token = userToken("c6buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-c6-" + System.nanoTime(), tierId, 2);
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);

        // 将支付期限改为已过期，触发超时关单
        expireOrder(orderId);

        orderCloseService.closeByTimeout(Long.valueOf(orderId));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, close_reason FROM ft_order WHERE id = ?", Long.valueOf(orderId));
        assertThat(row.get("status")).isEqualTo("CLOSED");
        assertThat(row.get("close_reason")).isEqualTo("PAYMENT_TIMEOUT");

        // 库存与限购恢复
        assertStock(Long.valueOf(tierId), 100, 100, 0, 0);
        assertLimit(userId, Long.valueOf(tierId), 0, 0);
    }

    @Test
    void timeoutCloseIsIdempotent() throws Exception {
        String tierId = publishSellingTier("c7", 100);
        registerUser("c7buyer", "c7buyer@test.com").andExpect(status().isOk());
        String token = userToken("c7buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-c7-" + System.nanoTime(), tierId, 1);
        expireOrder(orderId);

        orderCloseService.closeByTimeout(Long.valueOf(orderId));
        orderCloseService.closeByTimeout(Long.valueOf(orderId)); // 重复调度幂等

        assertStock(Long.valueOf(tierId), 100, 100, 0, 0);
        // 只有一条 TIMEOUT_CLOSED 流水（幂等不重复写）
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_order_operation WHERE order_id = ? AND operation = 'TIMEOUT_CLOSED'",
                Integer.class, Long.valueOf(orderId));
        assertThat(count).isEqualTo(1);
    }

    // ===== 操作流水 =====

    @Test
    void operationsLogReflectsLifecycle() throws Exception {
        String tierId = publishSellingTier("c8", 100);
        registerUser("c8buyer", "c8buyer@test.com").andExpect(status().isOk());
        String token = userToken("c8buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-c8-" + System.nanoTime(), tierId, 1);

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 流水应为：ORDER_CREATED → USER_CANCELLED
        mockMvc.perform(get("/api/v1/orders/{id}/operations", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].operation").value("ORDER_CREATED"))
                .andExpect(jsonPath("$.data[0].toStatus").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data[1].operation").value("USER_CANCELLED"))
                .andExpect(jsonPath("$.data[1].fromStatus").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data[1].toStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data[1].reason").value("USER_CANCEL"));
    }

    @Test
    void operationsLogRejectsCrossUserAccess() throws Exception {
        String tierId = publishSellingTier("c9", 100);
        registerUser("c9owner", "c9owner@test.com").andExpect(status().isOk());
        registerUser("c9other", "c9other@test.com").andExpect(status().isOk());
        String ownerToken = userToken("c9owner", PASSWORD);
        String otherToken = userToken("c9other", PASSWORD);

        String orderId = checkoutOrder(ownerToken, "req-c9-" + System.nanoTime(), tierId, 1);

        mockMvc.perform(get("/api/v1/orders/{id}/operations", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_OWNED"));
    }

    /* ---------- 辅助 ---------- */

    /** 抢票建单并返回订单 ID（M12 异步：受理后直调消费者建单）。 */
    private String checkoutOrder(String token, String requestId, String tierId, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestId", requestId, "tierId", tierId, "quantity", quantity))))
                .andExpect(status().isAccepted());
        String eventId = jdbcTemplate.queryForObject(
                "SELECT event_id FROM ft_outbox_event WHERE aggregate_id = ? ORDER BY id ASC LIMIT 1",
                String.class, requestId);
        assertThat(processor.process(eventId, requestId)).isTrue();
        return jdbcTemplate.queryForObject(
                "SELECT id FROM ft_order WHERE request_id = ?", String.class, requestId);
    }

    /** 将订单支付期限改为已过期（模拟超时）。 */
    private void expireOrder(String orderId) {
        jdbcTemplate.update("UPDATE ft_order SET expire_at = ? WHERE id = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), Long.valueOf(orderId));
    }

    private void assertStock(Long tierId, int total, int available, int reserved, int sold) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT total, available, reserved, sold FROM ft_inventory WHERE tier_id = ?", tierId);
        assertThat(((Number) row.get("total")).intValue()).as("total").isEqualTo(total);
        assertThat(((Number) row.get("available")).intValue()).as("available").isEqualTo(available);
        assertThat(((Number) row.get("reserved")).intValue()).as("reserved").isEqualTo(reserved);
        assertThat(((Number) row.get("sold")).intValue()).as("sold").isEqualTo(sold);
    }

    private void assertLimit(Long userId, Long tierId, int reserved, int sold) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT reserved_quantity, sold_quantity FROM ft_purchase_limit WHERE user_id = ? AND tier_id = ?",
                userId, tierId);
        assertThat(((Number) row.get("reserved_quantity")).intValue()).as("reserved").isEqualTo(reserved);
        assertThat(((Number) row.get("sold_quantity")).intValue()).as("sold").isEqualTo(sold);
    }

    private Long userIdOf(String username) {
        return userDomainService.findUserByAccount(username).getId();
    }

    private String userToken(String account, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("account", account, "password", password))))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("accessToken").asText();
    }

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

    private ResultActions adminLogin(String account, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("account", account, "password", password))));
    }

    private String accessToken(MvcResult loginResult) throws Exception {
        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    /** 发布一个「开售中」的演出，返回票档 ID。 */
    private String publishSellingTier(String prefix, int quantity) throws Exception {
        makeOperator(prefix + "op");
        makeSuperAdmin(prefix + "sa");
        String opToken = accessToken(adminLogin(prefix + "op", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin(prefix + "sa", PASSWORD).andReturn());
        String eventId = createFullEvent(opToken, "关单测试活动" + prefix);
        String venueId = createVenue(opToken, "关单场馆" + prefix, "北京", 5000);
        String sessionId = createSession(opToken, eventId, venueId, "晚场",
                "2027-06-01T19:00:00", "2027-06-01T21:00:00", 2000);
        String tierId = createTier(opToken, sessionId, "普通票", 100L, quantity);
        String[] window = sellingWindow();
        configureSalePlan(opToken, tierId, window[0], window[1]);

        postEvent(opToken, "/api/v1/admin/events/{id}/submit-review", eventId, null)
                .andExpect(status().isOk());
        postEvent(saToken, "/api/v1/admin/events/{id}/review", eventId, Map.of("approve", true))
                .andExpect(status().isOk());
        postEvent(saToken, "/api/v1/admin/events/{id}/publish", eventId, null)
                .andExpect(status().isOk());
        return tierId;
    }

    private String[] sellingWindow() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return new String[]{now.minusHours(1).format(FMT), now.plusHours(1).format(FMT)};
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
