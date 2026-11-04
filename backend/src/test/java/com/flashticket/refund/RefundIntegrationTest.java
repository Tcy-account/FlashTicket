package com.flashticket.refund;

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
 * M11 全额退款与权益撤销集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：申请退款（PENDING_REVIEW + 流水）、未支付/越权拒绝、重复申请幂等、
 * 用户撤销、财务审核通过自动执行退款（权益 REVOKED、订单保持 PAID、库存 sold 不变）、
 * 审核拒绝（意见必填）、自己审核自己拒绝、越权查询、管理端退款权限。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RefundIntegrationTest {

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

    // ===== 申请退款 =====

    @Test
    void applyRefundCreatesPendingReviewAndWritesOperation() throws Exception {
        String tierId = publishSellingTier("r1", 100);
        registerUser("r1buyer", "r1buyer@test.com").andExpect(status().isOk());
        String token = userToken("r1buyer", PASSWORD);

        String orderId = paidOrder(token, "req-r1-" + System.nanoTime(), tierId, 2);

        MvcResult applied = applyRefund(token, orderId, "ref-r1-" + System.nanoTime(), "PERSONAL", "行程有变")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn();
        String refundId = json(applied).get("id").asText();

        // 退款金额 = 原支付金额（200 分）
        Map<String, Object> refund = jdbcTemplate.queryForMap(
                "SELECT status, amount_cent FROM ft_refund WHERE id = ?", Long.valueOf(refundId));
        assertThat(refund.get("status")).isEqualTo("PENDING_REVIEW");
        assertThat(((Number) refund.get("amount_cent")).longValue()).isEqualTo(200L);

        // 写订单操作流水 REFUND_REQUESTED
        Integer opCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_order_operation WHERE order_id = ? AND operation = 'REFUND_REQUESTED'",
                Integer.class, Long.valueOf(orderId));
        assertThat(opCount).isEqualTo(1);
    }

    @Test
    void applyRefundRejectsUnpaidOrder() throws Exception {
        String tierId = publishSellingTier("r2", 100);
        registerUser("r2buyer", "r2buyer@test.com").andExpect(status().isOk());
        String token = userToken("r2buyer", PASSWORD);

        // 仅抢票未支付
        String orderId = checkoutOrder(token, "req-r2-" + System.nanoTime(), tierId, 1);

        applyRefund(token, orderId, "ref-r2-" + System.nanoTime(), "PERSONAL", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REFUND_ORDER_NOT_REFUNDABLE"));
    }

    @Test
    void applyRefundRejectsCrossUserAccess() throws Exception {
        String tierId = publishSellingTier("r3", 100);
        registerUser("r3owner", "r3owner@test.com").andExpect(status().isOk());
        registerUser("r3other", "r3other@test.com").andExpect(status().isOk());
        String ownerToken = userToken("r3owner", PASSWORD);
        String otherToken = userToken("r3other", PASSWORD);

        String orderId = paidOrder(ownerToken, "req-r3-" + System.nanoTime(), tierId, 1);

        applyRefund(otherToken, orderId, "ref-r3-" + System.nanoTime(), "PERSONAL", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_OWNED"));
    }

    @Test
    void applyRefundIsIdempotentForSameOrder() throws Exception {
        String tierId = publishSellingTier("r4", 100);
        registerUser("r4buyer", "r4buyer@test.com").andExpect(status().isOk());
        String token = userToken("r4buyer", PASSWORD);

        String orderId = paidOrder(token, "req-r4-" + System.nanoTime(), tierId, 1);

        String first = applyRefundId(token, orderId, "ref-r4a-" + System.nanoTime());
        String second = applyRefundId(token, orderId, "ref-r4b-" + System.nanoTime());

        // 一订单一退款业务：重复申请返回同一退款单
        assertThat(second).isEqualTo(first);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_refund WHERE order_id = ?", Integer.class, Long.valueOf(orderId));
        assertThat(count).isEqualTo(1);
    }

    // ===== 撤销 / 审核 =====

    @Test
    void cancelRefundMarksCancelled() throws Exception {
        String tierId = publishSellingTier("r5", 100);
        registerUser("r5buyer", "r5buyer@test.com").andExpect(status().isOk());
        String token = userToken("r5buyer", PASSWORD);

        String orderId = paidOrder(token, "req-r5-" + System.nanoTime(), tierId, 1);
        String refundId = applyRefundId(token, orderId, "ref-r5-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/refunds/{id}/cancel", refundId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void approveRefundAutoExecutesAndRevokesEntitlements() throws Exception {
        String tierId = publishSellingTier("r6", 100);
        registerUser("r6buyer", "r6buyer@test.com").andExpect(status().isOk());
        makeFinance("r6fin");
        Long userId = userIdOf("r6buyer");
        String token = userToken("r6buyer", PASSWORD);
        String finToken = accessToken(adminLogin("r6fin", PASSWORD).andReturn());

        // 购买 2 张并支付（生成 2 条权益）
        String orderId = paidOrder(token, "req-r6-" + System.nanoTime(), tierId, 2);
        assertStock(Long.valueOf(tierId), 100, 98, 0, 2);

        String refundId = applyRefundId(token, orderId, "ref-r6-" + System.nanoTime());

        // 财务审核通过 → 自动执行退款成功
        mockMvc.perform(post("/api/v1/admin/refunds/{id}/approve", refundId)
                        .header("Authorization", "Bearer " + finToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "同意退款"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.approvedBy").isNotEmpty());

        // 权益全部 REVOKED
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM ft_ticket_entitlement WHERE order_id = ?", String.class, Long.valueOf(orderId));
        assertThat(statuses).hasSize(2).containsOnly("REVOKED");

        // 订单保持 PAID，库存 sold 不变（不自动返还）
        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT status FROM ft_order WHERE id = ?", Long.valueOf(orderId));
        assertThat(order.get("status")).isEqualTo("PAID");
        assertStock(Long.valueOf(tierId), 100, 98, 0, 2);

        // 写订单操作流水 REFUND_SUCCEEDED
        Integer opCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_order_operation WHERE order_id = ? AND operation = 'REFUND_SUCCEEDED'",
                Integer.class, Long.valueOf(orderId));
        assertThat(opCount).isEqualTo(1);
    }

    @Test
    void rejectRefundRequiresCommentAndMarksRejected() throws Exception {
        String tierId = publishSellingTier("r7", 100);
        registerUser("r7buyer", "r7buyer@test.com").andExpect(status().isOk());
        makeFinance("r7fin");
        String token = userToken("r7buyer", PASSWORD);
        String finToken = accessToken(adminLogin("r7fin", PASSWORD).andReturn());

        String orderId = paidOrder(token, "req-r7-" + System.nanoTime(), tierId, 1);
        String refundId = applyRefundId(token, orderId, "ref-r7-" + System.nanoTime());

        // 拒绝但不填意见 → 400
        mockMvc.perform(post("/api/v1/admin/refunds/{id}/reject", refundId)
                        .header("Authorization", "Bearer " + finToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REFUND_REVIEW_COMMENT_REQUIRED"));

        // 拒绝并填意见 → REJECTED
        mockMvc.perform(post("/api/v1/admin/refunds/{id}/reject", refundId)
                        .header("Authorization", "Bearer " + finToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "不符合退款条件"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        // 权益保持 VALID（拒绝不撤销权益）
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM ft_ticket_entitlement WHERE order_id = ?", String.class, Long.valueOf(orderId));
        assertThat(statuses).containsOnly("VALID");
    }

    @Test
    void approveRefundRejectsSelfReview() throws Exception {
        String tierId = publishSellingTier("r8", 100);
        // 财务账号兼具 USER 角色，可自行购票
        makeFinance("r8fin");
        String token = userToken("r8fin", PASSWORD);
        String finToken = accessToken(adminLogin("r8fin", PASSWORD).andReturn());

        String orderId = paidOrder(token, "req-r8-" + System.nanoTime(), tierId, 1);
        String refundId = applyRefundId(token, orderId, "ref-r8-" + System.nanoTime());

        // 审核自己提交的退款申请 → 禁止
        mockMvc.perform(post("/api/v1/admin/refunds/{id}/approve", refundId)
                        .header("Authorization", "Bearer " + finToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REFUND_SELF_REVIEW"));
    }

    // ===== 查询与权限 =====

    @Test
    void getRefundRequiresOwnership() throws Exception {
        String tierId = publishSellingTier("r9", 100);
        registerUser("r9owner", "r9owner@test.com").andExpect(status().isOk());
        registerUser("r9other", "r9other@test.com").andExpect(status().isOk());
        String ownerToken = userToken("r9owner", PASSWORD);
        String otherToken = userToken("r9other", PASSWORD);

        String orderId = paidOrder(ownerToken, "req-r9-" + System.nanoTime(), tierId, 1);
        String refundId = applyRefundId(ownerToken, orderId, "ref-r9-" + System.nanoTime());

        // 本人可查
        mockMvc.perform(get("/api/v1/refunds/{id}", refundId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // 他人查询 → 403 越权
        mockMvc.perform(get("/api/v1/refunds/{id}", refundId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REFUND_NOT_OWNED"));
    }

    @Test
    void adminRefundsRequireRefundPermission() throws Exception {
        // FINANCE 有 refund:read/approve，OPERATOR 没有
        makeFinance("r10fin");
        makeOperator("r10op");
        String finToken = accessToken(adminLogin("r10fin", PASSWORD).andReturn());
        String opToken = accessToken(adminLogin("r10op", PASSWORD).andReturn());

        mockMvc.perform(get("/api/v1/admin/refunds")
                        .header("Authorization", "Bearer " + finToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/refunds")
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isForbidden());
    }

    /* ---------- 退款辅助 ---------- */

    private String paidOrder(String token, String requestId, String tierId, int quantity) throws Exception {
        String orderId = checkoutOrder(token, requestId, tierId, quantity);
        String paymentId = createPayment(token, orderId);
        mockPay(token, paymentId, "att-" + System.nanoTime()).andExpect(status().isOk());
        return orderId;
    }

    private ResultActions applyRefund(String token, String orderId, String requestId,
                                      String reasonCode, String reasonDetail) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("refundRequestId", requestId);
        body.put("reasonCode", reasonCode);
        if (reasonDetail != null) {
            body.put("reasonDetail", reasonDetail);
        }
        return mockMvc.perform(post("/api/v1/orders/{id}/refunds", orderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String applyRefundId(String token, String orderId, String requestId) throws Exception {
        MvcResult applied = applyRefund(token, orderId, requestId, "PERSONAL", null)
                .andExpect(status().isOk())
                .andReturn();
        return json(applied).get("id").asText();
    }

    /* ---------- 通用辅助（与 PaymentIntegrationTest 同构） ---------- */

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

    private void assertStock(Long tierId, int total, int available, int reserved, int sold) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT total, available, reserved, sold FROM ft_inventory WHERE tier_id = ?", tierId);
        assertThat(((Number) row.get("total")).intValue()).as("total").isEqualTo(total);
        assertThat(((Number) row.get("available")).intValue()).as("available").isEqualTo(available);
        assertThat(((Number) row.get("reserved")).intValue()).as("reserved").isEqualTo(reserved);
        assertThat(((Number) row.get("sold")).intValue()).as("sold").isEqualTo(sold);
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
        String eventId = createFullEvent(opToken, "退款测试活动" + prefix);
        String venueId = createVenue(opToken, "退款场馆" + prefix, "上海", 5000);
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
