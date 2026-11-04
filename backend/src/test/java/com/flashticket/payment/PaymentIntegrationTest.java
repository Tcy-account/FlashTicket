package com.flashticket.payment;

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
 * M10 模拟支付与票务权益集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：支付成功订单 PAID + 库存 reserved→sold + 限购转 sold + 生成对应数量权益、
 * 支付单创建幂等、重复支付幂等不重复确认、关单/过期订单拒绝支付、越权访问、
 * 我的门票列表与归属校验、管理端支付查询权限。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PaymentIntegrationTest {

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

    // ===== 支付成功主链路 =====

    @Test
    void paySuccessfullyTransitionsOrderInventoryAndIssuesEntitlements() throws Exception {
        String tierId = publishSellingTier("p1", 100);
        registerUser("p1buyer", "p1buyer@test.com").andExpect(status().isOk());
        Long userId = userIdOf("p1buyer");
        String token = userToken("p1buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-p1-" + System.nanoTime(), tierId, 2);
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);
        assertLimit(userId, Long.valueOf(tierId), 2, 0);

        // 创建支付单
        String paymentId = createPayment(token, orderId);
        assertThat(paymentId).isNotBlank();

        // 模拟支付
        mockPay(token, paymentId, "att-p1-" + System.nanoTime())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        // 订单 PAID，实付金额 = 应付
        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT status, paid_amount_cent FROM ft_order WHERE id = ?", Long.valueOf(orderId));
        assertThat(order.get("status")).isEqualTo("PAID");
        assertThat(((Number) order.get("paid_amount_cent")).longValue()).isEqualTo(200L);

        // 库存 reserved→sold
        assertStock(Long.valueOf(tierId), 100, 98, 0, 2);
        assertLimit(userId, Long.valueOf(tierId), 0, 2);

        // 生成 2 条有效权益
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM ft_ticket_entitlement WHERE order_id = ?", String.class, Long.valueOf(orderId));
        assertThat(statuses).hasSize(2).containsOnly("VALID");
    }

    @Test
    void createOrGetPaymentIsIdempotent() throws Exception {
        String tierId = publishSellingTier("p2", 100);
        registerUser("p2buyer", "p2buyer@test.com").andExpect(status().isOk());
        String token = userToken("p2buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-p2-" + System.nanoTime(), tierId, 1);

        String first = createPayment(token, orderId);
        String second = createPayment(token, orderId);

        // 一订单一支付单：重复创建返回同一支付单
        assertThat(second).isEqualTo(first);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_payment WHERE order_id = ?", Integer.class, Long.valueOf(orderId));
        assertThat(count).isEqualTo(1);
    }

    @Test
    void mockPayIsIdempotentWhenAlreadySucceeded() throws Exception {
        String tierId = publishSellingTier("p3", 100);
        registerUser("p3buyer", "p3buyer@test.com").andExpect(status().isOk());
        Long userId = userIdOf("p3buyer");
        String token = userToken("p3buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-p3-" + System.nanoTime(), tierId, 1);
        String paymentId = createPayment(token, orderId);

        mockPay(token, paymentId, "att-p3a-" + System.nanoTime())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        // 重复支付（不同 attemptRequestId）：支付单已成功，幂等返回，不重复确认
        mockPay(token, paymentId, "att-p3b-" + System.nanoTime())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        // 库存 sold 不重复增加
        assertStock(Long.valueOf(tierId), 100, 99, 0, 1);
        assertLimit(userId, Long.valueOf(tierId), 0, 1);
        Integer entitlements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_ticket_entitlement WHERE order_id = ?", Integer.class, Long.valueOf(orderId));
        assertThat(entitlements).isEqualTo(1);
    }

    // ===== 拒绝非法支付 =====

    @Test
    void createPaymentRejectsClosedOrder() throws Exception {
        String tierId = publishSellingTier("p4", 100);
        registerUser("p4buyer", "p4buyer@test.com").andExpect(status().isOk());
        String token = userToken("p4buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-p4-" + System.nanoTime(), tierId, 1);

        // 关单后不可再创建支付单
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_ORDER_NOT_PAYABLE"));
    }

    @Test
    void createPaymentRejectsExpiredOrder() throws Exception {
        String tierId = publishSellingTier("p5", 100);
        registerUser("p5buyer", "p5buyer@test.com").andExpect(status().isOk());
        String token = userToken("p5buyer", PASSWORD);

        String orderId = checkoutOrder(token, "req-p5-" + System.nanoTime(), tierId, 1);
        expireOrder(orderId);

        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_ORDER_EXPIRED"));
    }

    @Test
    void mockPayRejectsCrossUserAccess() throws Exception {
        String tierId = publishSellingTier("p6", 100);
        registerUser("p6owner", "p6owner@test.com").andExpect(status().isOk());
        registerUser("p6other", "p6other@test.com").andExpect(status().isOk());
        String ownerToken = userToken("p6owner", PASSWORD);
        String otherToken = userToken("p6other", PASSWORD);

        String orderId = checkoutOrder(ownerToken, "req-p6-" + System.nanoTime(), tierId, 1);
        String paymentId = createPayment(ownerToken, orderId);

        // 他人操作支付单 → 403 越权，订单仍待支付
        mockPay(otherToken, paymentId, "att-p6-" + System.nanoTime())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_OWNED"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status FROM ft_order WHERE id = ?", Long.valueOf(orderId));
        assertThat(row.get("status")).isEqualTo("PENDING_PAYMENT");
    }

    // ===== 票务权益 =====

    @Test
    void myEntitlementsListAndDetailWithOwnershipCheck() throws Exception {
        String tierId = publishSellingTier("p7", 100);
        registerUser("p7buyer", "p7buyer@test.com").andExpect(status().isOk());
        registerUser("p7other", "p7other@test.com").andExpect(status().isOk());
        String token = userToken("p7buyer", PASSWORD);
        String otherToken = userToken("p7other", PASSWORD);

        String orderId = checkoutOrder(token, "req-p7-" + System.nanoTime(), tierId, 2);
        String paymentId = createPayment(token, orderId);
        mockPay(token, paymentId, "att-p7-" + System.nanoTime()).andExpect(status().isOk());

        // 我的门票列表：2 条有效权益
        MvcResult list = mockMvc.perform(get("/api/v1/entitlements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("VALID"))
                .andReturn();

        String entitlementId = json(list).get(0).get("id").asText();

        // 详情（本人可查）
        mockMvc.perform(get("/api/v1/entitlements/{id}", entitlementId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entitlementNo").isNotEmpty());

        // 他人查看 → 403 越权
        mockMvc.perform(get("/api/v1/entitlements/{id}", entitlementId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ENTITLEMENT_NOT_OWNED"));
    }

    // ===== 管理端支付查询 =====

    @Test
    void adminPaymentsRequirePaymentReadPermission() throws Exception {
        // FINANCE 有 payment:read，OPERATOR 没有
        makeFinance("p8fin");
        makeOperator("p8op");
        String finToken = accessToken(adminLogin("p8fin", PASSWORD).andReturn());
        String opToken = accessToken(adminLogin("p8op", PASSWORD).andReturn());

        mockMvc.perform(get("/api/v1/admin/payments")
                        .header("Authorization", "Bearer " + finToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/payments")
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isForbidden());
    }

    /* ---------- 支付辅助 ---------- */

    private String createPayment(String token, String orderId) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return json(r).get("id").asText();
    }

    private ResultActions mockPay(String token, String paymentId, String attemptRequestId) throws Exception {
        return mockMvc.perform(post("/api/v1/payments/{id}/mock-pay", paymentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("attemptRequestId", attemptRequestId))));
    }

    /* ---------- 通用辅助（与 OrderCloseIntegrationTest 同构） ---------- */

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

    private void makeFinance(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        rbacDomainService.assignUserRoles(user.getId(),
                List.of(rbacDomainService.findRoleByCode("USER").getId(),
                        rbacDomainService.findRoleByCode("FINANCE").getId()));
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

    private String publishSellingTier(String prefix, int quantity) throws Exception {
        makeOperator(prefix + "op");
        makeSuperAdmin(prefix + "sa");
        String opToken = accessToken(adminLogin(prefix + "op", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin(prefix + "sa", PASSWORD).andReturn());
        String eventId = createFullEvent(opToken, "支付测试活动" + prefix);
        String venueId = createVenue(opToken, "支付场馆" + prefix, "上海", 5000);
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
