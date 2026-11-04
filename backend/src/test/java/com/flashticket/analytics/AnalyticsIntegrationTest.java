package com.flashticket.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * M16-A 运营统计集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：平台概览聚合口径（实收/票数/订单）、退款统计口径、抢票转化统计、
 * analytics:read 权限。采用「差值断言」规避类内多测试方法数据累积。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

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

    @Test
    void overviewReflectsPaidOrder() throws Exception {
        String tierId = publishSellingTier("ana1", 100);
        String saToken = accessToken(adminLogin("ana1sa", PASSWORD).andReturn());
        registerUser("ana1buyer", "ana1buyer@test.com").andExpect(status().isOk());
        String token = userToken("ana1buyer", PASSWORD);

        JsonNode base = overview(saToken);
        long basePaid = base.get("paidAmountCent").asLong();
        long baseTicket = base.get("soldTicketCount").asLong();
        long basePaidOrder = base.get("paidOrderCount").asLong();
        long baseOrder = base.get("orderCount").asLong();

        // 支付一单 2 张 × 100 分 = 200 分实收
        paidOrder(token, "req-ana1-" + System.nanoTime(), tierId, 2);

        JsonNode after = overview(saToken);
        assertThat(after.get("paidAmountCent").asLong() - basePaid).isEqualTo(200L);
        assertThat(after.get("soldTicketCount").asLong() - baseTicket).isEqualTo(2L);
        assertThat(after.get("paidOrderCount").asLong() - basePaidOrder).isEqualTo(1L);
        assertThat(after.get("orderCount").asLong() - baseOrder).isEqualTo(1L);
        assertThat(after.get("netRevenueCent").asLong())
                .isEqualTo(after.get("paidAmountCent").asLong() - after.get("refundAmountCent").asLong());
        assertThat(after.get("dailyTrend").isArray()).isTrue();
    }

    @Test
    void overviewReflectsRefund() throws Exception {
        String tierId = publishSellingTier("ana2", 100);
        String saToken = accessToken(adminLogin("ana2sa", PASSWORD).andReturn());
        makeFinance("ana2fin");
        registerUser("ana2buyer", "ana2buyer@test.com").andExpect(status().isOk());
        String token = userToken("ana2buyer", PASSWORD);
        String finToken = accessToken(adminLogin("ana2fin", PASSWORD).andReturn());

        String orderId = paidOrder(token, "req-ana2-" + System.nanoTime(), tierId, 1);

        JsonNode base = overview(saToken);
        long baseRefund = base.get("refundAmountCent").asLong();
        long baseRefundOrder = base.get("refundedOrderCount").asLong();

        // 申请退款 + 财务审核通过 → SUCCEEDED
        String refundId = applyRefundId(token, orderId, "ref-ana2-" + System.nanoTime());
        mockMvc.perform(post("/api/v1/admin/refunds/{id}/approve", refundId)
                        .header("Authorization", "Bearer " + finToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "同意退款"))))
                .andExpect(status().isOk());

        JsonNode after = overview(saToken);
        assertThat(after.get("refundAmountCent").asLong() - baseRefund).isEqualTo(100L);
        assertThat(after.get("refundedOrderCount").asLong() - baseRefundOrder).isEqualTo(1L);
    }

    @Test
    void purchaseConversionCountsSucceeded() throws Exception {
        String tierId = publishSellingTier("ana3", 100);
        String saToken = accessToken(adminLogin("ana3sa", PASSWORD).andReturn());
        registerUser("ana3buyer", "ana3buyer@test.com").andExpect(status().isOk());
        String token = userToken("ana3buyer", PASSWORD);

        JsonNode base = purchases(saToken);
        long baseSucceeded = base.get("succeededCount").asLong();
        long baseAccepted = base.get("acceptedCount").asLong();

        checkoutOrder(token, "req-ana3-" + System.nanoTime(), tierId, 1);

        JsonNode after = purchases(saToken);
        assertThat(after.get("succeededCount").asLong() - baseSucceeded).isEqualTo(1L);
        assertThat(after.get("acceptedCount").asLong() - baseAccepted).isEqualTo(1L);
        long done = after.get("succeededCount").asLong() + after.get("failedCount").asLong();
        double expected = done == 0 ? 0.0 : (double) after.get("succeededCount").asLong() / done;
        assertThat(after.get("successRate").asDouble()).isCloseTo(expected, within(0.01));
    }

    @Test
    void analyticsRequiresReadPermission() throws Exception {
        registerUser("ana4user", "ana4user@test.com").andExpect(status().isOk());
        String userToken = userToken("ana4user", PASSWORD);
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().is4xxClientError());

        makeOperator("ana4op");
        String opToken = accessToken(adminLogin("ana4op", PASSWORD).andReturn());
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk());
    }

    /* ---------- 统计查询辅助 ---------- */

    private JsonNode overview(String token) throws Exception {
        return json(mockMvc.perform(get("/api/v1/admin/analytics/overview")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn());
    }

    private JsonNode purchases(String token) throws Exception {
        return json(mockMvc.perform(get("/api/v1/admin/analytics/purchases")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn());
    }

    /* ---------- 交易链路辅助 ---------- */

    private String paidOrder(String token, String requestId, String tierId, int quantity) throws Exception {
        String orderId = checkoutOrder(token, requestId, tierId, quantity);
        String paymentId = createPayment(token, orderId);
        mockPay(token, paymentId, "att-" + System.nanoTime()).andExpect(status().isOk());
        return orderId;
    }

    private String createPayment(String token, String orderId) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
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

    private String applyRefundId(String token, String orderId, String requestId) throws Exception {
        MvcResult applied = mockMvc.perform(post("/api/v1/orders/{id}/refunds", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refundRequestId", requestId, "reasonCode", "PERSONAL"))))
                .andExpect(status().isOk()).andReturn();
        return json(applied).get("id").asText();
    }

    /* ---------- 通用辅助（与 RefundIntegrationTest 同构） ---------- */

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
        String eventId = createFullEvent(opToken, "统计测试活动" + prefix);
        String venueId = createVenue(opToken, "统计场馆" + prefix, "上海", 5000);
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
                return mockMvc.perform(post(path, eventId).header("Authorization", "Bearer " + token));
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
                .andExpect(status().isOk()).andReturn();
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
                .andExpect(status().isOk()).andReturn();
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
                .andExpect(status().isOk()).andReturn();
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
                .andExpect(status().isOk()).andReturn();
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
