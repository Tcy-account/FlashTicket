package com.flashticket.reconciliation;

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
import com.flashticket.reconciliation.entity.ReconcileJob;
import com.flashticket.reconciliation.enums.ReconcileScope;
import com.flashticket.reconciliation.service.ReconciliationService;
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
 * M16-B 系统对账集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>通过受控故障（UPDATE 制造不一致状态）验证对账能定位差异；守恒/重复支付/重复退款
 * 类异常由数据库约束（CHECK/UNIQUE）兜底，正常 DML 无法制造，不在此重复验证。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ReconciliationIntegrationTest {

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
    private ReconciliationService reconciliationService;

    @Test
    void orderReconcileDetectsExpiredPending() throws Exception {
        String tierId = publishSellingTier("rec1", 100);
        registerUser("rec1buyer", "rec1buyer@test.com").andExpect(status().isOk());
        String token = userToken("rec1buyer", PASSWORD);

        // 抢票未支付（PENDING_PAYMENT），再把支付期限改到过去
        String orderId = checkoutOrder(token, "req-rec1-" + System.nanoTime(), tierId, 1);
        jdbcTemplate.update("UPDATE ft_order SET expire_at = ? WHERE id = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1), Long.valueOf(orderId));

        ReconcileJob job = reconciliationService.runReconcile(ReconcileScope.ORDER, null);

        assertThat(job.getStatus()).isEqualTo("SUCCEEDED");
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_reconcile_exception "
                        + "WHERE check_type = 'ORDER_EXPIRED_PENDING' AND entity_id = ?",
                Integer.class, orderId);
        assertThat(cnt).isEqualTo(1);
    }

    @Test
    void paymentReconcileDetectsMissingSuccessPayment() throws Exception {
        String tierId = publishSellingTier("rec2", 100);
        registerUser("rec2buyer", "rec2buyer@test.com").andExpect(status().isOk());
        String token = userToken("rec2buyer", PASSWORD);

        // 正常支付出 PAID 订单 + SUCCEEDED 支付，再把支付单改成 FAILED（模拟支付记录丢失）
        String orderId = paidOrder(token, "req-rec2-" + System.nanoTime(), tierId, 1);
        jdbcTemplate.update("UPDATE ft_payment SET status = 'FAILED' WHERE order_id = ?",
                Long.valueOf(orderId));

        ReconcileJob job = reconciliationService.runReconcile(ReconcileScope.PAYMENT, null);

        assertThat(job.getStatus()).isEqualTo("SUCCEEDED");
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_reconcile_exception "
                        + "WHERE check_type = 'PAYMENT_MISSING' AND entity_id = ?",
                Integer.class, orderId);
        assertThat(cnt).isEqualTo(1);
    }

    @Test
    void inventoryReconcilePassesOnValidData() throws Exception {
        publishSellingTier("rec3", 100);
        ReconcileJob job = reconciliationService.runReconcile(ReconcileScope.INVENTORY, null);
        assertThat(job.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(job.getExceptionCount()).isZero();
    }

    @Test
    void reconcileIsIdempotentForOpenException() throws Exception {
        String tierId = publishSellingTier("rec4", 100);
        registerUser("rec4buyer", "rec4buyer@test.com").andExpect(status().isOk());
        String token = userToken("rec4buyer", PASSWORD);
        String orderId = checkoutOrder(token, "req-rec4-" + System.nanoTime(), tierId, 1);
        jdbcTemplate.update("UPDATE ft_order SET expire_at = ? WHERE id = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1), Long.valueOf(orderId));

        ReconcileJob first = reconciliationService.runReconcile(ReconcileScope.ORDER, null);
        ReconcileJob second = reconciliationService.runReconcile(ReconcileScope.ORDER, null);

        assertThat(first.getExceptionCount()).isGreaterThanOrEqualTo(1);
        // 同实体 OPEN 工单已存在，第二次不重复生成
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_reconcile_exception "
                        + "WHERE check_type = 'ORDER_EXPIRED_PENDING' AND entity_id = ?",
                Integer.class, orderId);
        assertThat(total).isEqualTo(1);
        assertThat(second.getExceptionCount()).isZero();
    }

    @Test
    void adminReconciliationApiCreatesJobAndListsExceptions() throws Exception {
        String tierId = publishSellingTier("rec5", 100);
        String saToken = accessToken(adminLogin("rec5sa", PASSWORD).andReturn());
        registerUser("rec5buyer", "rec5buyer@test.com").andExpect(status().isOk());
        String token = userToken("rec5buyer", PASSWORD);
        String orderId = checkoutOrder(token, "req-rec5-" + System.nanoTime(), tierId, 1);
        jdbcTemplate.update("UPDATE ft_order SET expire_at = ? WHERE id = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1), Long.valueOf(orderId));

        mockMvc.perform(post("/api/v1/admin/reconciliation/jobs")
                        .param("scope", "ORDER")
                        .header("Authorization", "Bearer " + saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        mockMvc.perform(get("/api/v1/admin/reconciliation/exceptions")
                        .header("Authorization", "Bearer " + saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].checkType").value("ORDER_EXPIRED_PENDING"));
    }

    @Test
    void reviewResolvesException() throws Exception {
        String tierId = publishSellingTier("rec6", 100);
        String saToken = accessToken(adminLogin("rec6sa", PASSWORD).andReturn());
        registerUser("rec6buyer", "rec6buyer@test.com").andExpect(status().isOk());
        String token = userToken("rec6buyer", PASSWORD);
        String orderId = checkoutOrder(token, "req-rec6-" + System.nanoTime(), tierId, 1);
        jdbcTemplate.update("UPDATE ft_order SET expire_at = ? WHERE id = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1), Long.valueOf(orderId));
        reconciliationService.runReconcile(ReconcileScope.ORDER, null);

        Long exceptionId = jdbcTemplate.queryForObject(
                "SELECT id FROM ft_reconcile_exception WHERE check_type = 'ORDER_EXPIRED_PENDING' AND entity_id = ?",
                Long.class, orderId);

        mockMvc.perform(post("/api/v1/admin/reconciliation/exceptions/{id}/review", exceptionId)
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resolution", "FALSE_POSITIVE", "comment", "误报"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolution").value("FALSE_POSITIVE"));

        Integer actions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_reconcile_action WHERE exception_id = ?",
                Integer.class, exceptionId);
        assertThat(actions).isEqualTo(1);
    }

    @Test
    void closeOrderCompensationSucceeds() throws Exception {
        String tierId = publishSellingTier("rec7", 100);
        String saToken = accessToken(adminLogin("rec7sa", PASSWORD).andReturn());
        registerUser("rec7buyer", "rec7buyer@test.com").andExpect(status().isOk());
        String token = userToken("rec7buyer", PASSWORD);
        String orderId = checkoutOrder(token, "req-rec7-" + System.nanoTime(), tierId, 1);

        MvcResult created = mockMvc.perform(post("/api/v1/admin/compensations")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "taskType", "CLOSE_ORDER", "entityType", "ORDER", "entityId", orderId))))
                .andExpect(status().isOk()).andReturn();
        String taskId = json(created).get("id").asText();

        mockMvc.perform(post("/api/v1/admin/compensations/{id}/execute", taskId)
                        .header("Authorization", "Bearer " + saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT status FROM ft_order WHERE id = ?", Long.valueOf(orderId));
        assertThat(order.get("status")).isEqualTo("CLOSED");
    }

    @Test
    void compensationIsIdempotent() throws Exception {
        makeSuperAdmin("rec8sa");
        String saToken = accessToken(adminLogin("rec8sa", PASSWORD).andReturn());

        String body = objectMapper.writeValueAsString(Map.of(
                "taskType", "CLOSE_ORDER", "entityType", "ORDER", "entityId", "999999"));
        MvcResult first = mockMvc.perform(post("/api/v1/admin/compensations")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/admin/compensations")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();

        assertThat(json(first).get("id").asText()).isEqualTo(json(second).get("id").asText());
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
        String eventId = createFullEvent(opToken, "对账测试活动" + prefix);
        String venueId = createVenue(opToken, "对账场馆" + prefix, "上海", 5000);
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
