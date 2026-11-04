package com.flashticket.notification;

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
import com.flashticket.notification.dto.NotificationEvent;
import com.flashticket.notification.service.NotificationProcessor;
import com.flashticket.notification.service.StreamTokenService;
import com.flashticket.purchase.service.PurchaseRequestProcessor;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * M14 系统通知集成测试：消息查询/已读、消费幂等、业务事件生成通知、SSE 凭证。
 *
 * <p>test profile 关闭 MQ listener，消费通过直调 {@link NotificationProcessor#process}
 * 完成；业务事件（抢票/支付）在终态事务内写 Outbox，测试取出后直调 processor 消费。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class NotificationIntegrationTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String PASSWORD = "M13Coupon@2026";
    private static final String CODE_KEY_PREFIX = "auth:verify:code:";

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
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserDomainService userDomainService;
    @Autowired
    private RbacDomainService rbacDomainService;
    @Autowired
    private PurchaseRequestProcessor purchaseProcessor;
    @Autowired
    private NotificationProcessor notificationProcessor;
    @Autowired
    private StreamTokenService streamTokenService;

    /* ================= 测试用例 ================= */

    @Test
    void messageQueryAndReadLifecycle() throws Exception {
        String userToken = makeUser("mknotifbuyer");
        Long userId = userIdOf("mknotifbuyer");

        // 直接消费一条支付成功事件，生成通知
        notificationProcessor.process(new NotificationEvent(
                UUID.randomUUID().toString(), "PAYMENT_SUCCESS", userId, "1001"));

        // 未读数 = 1
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // 列表含 1 条未读，标题/正文正确
        JsonNode list = json(mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn());
        assertThat(list.get("items").size()).isEqualTo(1);
        JsonNode item = list.get("items").get(0);
        assertThat(item.get("title").asText()).isEqualTo("支付成功");
        assertThat(item.get("read").asBoolean()).isFalse();
        assertThat(list.get("unreadCount").asInt()).isEqualTo(1);

        String recipientId = item.get("id").asText();

        // 单条已读
        mockMvc.perform(put("/api/v1/notifications/" + recipientId + "/read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        // 详情（归属校验：他人查自己的通知应 403）
        String otherToken = makeUser("mknotifother");
        mockMvc.perform(get("/api/v1/notifications/" + recipientId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/notifications/" + recipientId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void processIsIdempotent() throws Exception {
        makeUser("mknotifidem");
        Long userId = userIdOf("mknotifidem");
        String eventId = UUID.randomUUID().toString();

        // 同一事件重复消费 5 次，只生成一条通知
        for (int i = 0; i < 5; i++) {
            notificationProcessor.process(new NotificationEvent(
                    eventId, "ORDER_CLOSED", userId, "1002"));
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_notification WHERE event_id = ?", Integer.class, eventId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void purchaseAndPaymentProduceNotifications() throws Exception {
        String prefix = "notifflow";
        String userToken = makeUser(prefix + "buyer");
        Long userId = userIdOf(prefix + "buyer");
        String tierId = publishSellingTier(prefix);

        // 抢票成功 → 建单事务内写 PURCHASE_SUCCESS 通知 Outbox
        String requestId = UUID.randomUUID().toString();
        checkout(userToken, requestId, tierId, 1).andExpect(status().isAccepted());
        purchaseProcessor.process(eventIdOf(requestId), requestId);

        String orderId = orderIdOf(requestId);
        NotificationEvent purchaseEvent = latestNotificationEvent(orderId, "PURCHASE_SUCCESS");
        assertThat(purchaseEvent).isNotNull();
        assertThat(purchaseEvent.userId()).isEqualTo(userId);
        notificationProcessor.process(purchaseEvent);

        // 支付成功 → 支付事务内写 PAYMENT_SUCCESS 通知 Outbox
        pay(userToken, orderId);
        NotificationEvent paymentEvent = latestNotificationEvent(orderId, "PAYMENT_SUCCESS");
        assertThat(paymentEvent).isNotNull();
        notificationProcessor.process(paymentEvent);

        // 两条通知落库
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_notification n JOIN ft_notification_recipient r ON r.notification_id = n.id WHERE r.user_id = ?",
                Integer.class, userId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void streamTokenValidation() throws Exception {
        String userToken = makeUser("mknotifstream");
        Long userId = userIdOf("mknotifstream");

        // 签发凭证后可解析到 userId
        StreamTokenService.StreamTokenInfo info = streamTokenService.issue(userId);
        assertThat(streamTokenService.resolve(info.token())).isEqualTo(userId);

        // 无效凭证返回 null
        assertThat(streamTokenService.resolve("bad-token")).isNull();

        // 无凭证直接访问 SSE 流 → 401
        mockMvc.perform(get("/api/v1/notifications/stream").param("token", "bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_STREAM_TOKEN_INVALID"));

        // 未登录换取凭证 → 401（stream-token 需认证）
        mockMvc.perform(post("/api/v1/notifications/stream-token"))
                .andExpect(status().isUnauthorized());
    }

    /* ================= 辅助方法 ================= */

    private String publishSellingTier(String prefix) throws Exception {
        String opToken = operatorToken(prefix + "op", prefix + "sa");
        String eventId = createFullEvent(opToken, "演出" + prefix);
        String venueId = createVenue(opToken, "场馆" + prefix);
        String sessionId = createSession(opToken, eventId, venueId);
        String tierId = createTier(opToken, sessionId, 400L, 100);
        configureSalePlan(opToken, tierId);
        postEvent(opToken, "/api/v1/admin/events/" + eventId + "/submit-review", null).andExpect(status().isOk());
        String saToken = accessToken(adminLogin(prefix + "sa", PASSWORD).andReturn());
        postEvent(saToken, "/api/v1/admin/events/" + eventId + "/review", Map.of("approve", true)).andExpect(status().isOk());
        postEvent(saToken, "/api/v1/admin/events/" + eventId + "/publish", null).andExpect(status().isOk());
        return tierId;
    }

    private String createVenue(String token, String name) throws Exception {
        return json(mockMvc.perform(post("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "countryCode", "CN", "city", "北京",
                                "address", "某路1号", "timezone", "Asia/Shanghai", "maxCapacity", 5000))))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
    }

    private String createFullEvent(String token, String title) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("categoryId", "1");
        body.put("title", title);
        body.put("version", 0);
        body.put("coverUrl", "https://img.example.com/c.jpg");
        body.put("detailContent", "详情");
        body.put("organizerName", "主办方");
        body.put("attendanceNotice", "提前入场");
        return json(mockMvc.perform(post("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
    }

    private String createSession(String token, String eventId, String venueId) throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return json(mockMvc.perform(post("/api/v1/admin/events/" + eventId + "/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "venueId", venueId, "name", "晚场",
                                "startAt", now.plusDays(2).format(FMT),
                                "endAt", now.plusDays(2).plusHours(2).format(FMT),
                                "saleCapacity", 2000))))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
    }

    private String createTier(String token, String sessionId, long priceCent, int quantity) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "普通票");
        body.put("unitPriceCent", priceCent);
        body.put("initialQuantity", quantity);
        body.put("minPurchase", 1);
        body.put("maxPurchase", 6);
        body.put("perUserLimit", 10);
        return json(mockMvc.perform(post("/api/v1/admin/sessions/" + sessionId + "/tiers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
    }

    private void configureSalePlan(String token, String tierId) throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        mockMvc.perform(put("/api/v1/admin/tiers/" + tierId + "/sale-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "saleStartAt", now.minusHours(1).format(FMT),
                                "saleEndAt", now.plusHours(1).format(FMT)))))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions checkout(String token, String requestId,
                                                                        String tierId, int quantity)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("requestId", requestId);
        body.put("tierId", tierId);
        body.put("quantity", quantity);
        return mockMvc.perform(post("/api/v1/purchases")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private void pay(String userToken, String orderId) throws Exception {
        String paymentId = json(mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/mock-pay")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("attemptRequestId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());
    }

    /* ----- 身份与通用 ----- */

    private String operatorToken(String opName, String saName) throws Exception {
        makeOperator(opName);
        makeSuperAdmin(saName);
        return accessToken(adminLogin(opName, PASSWORD).andReturn());
    }

    private String makeUser(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        return userToken(username, PASSWORD);
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

    private String userToken(String account, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("account", account, "password", password))))
                .andReturn();
        return accessToken(r);
    }

    private org.springframework.test.web.servlet.ResultActions adminLogin(String account, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("account", account, "password", password))));
    }

    private String accessToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions registerUser(String username, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "purpose", "REGISTER"))))
                .andExpect(status().isOk());
        String code = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + "REGISTER:" + email.toLowerCase(Locale.ROOT));
        assertThat(code).isNotNull();
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username, "email", email, "password", PASSWORD, "verifyCode", code))));
    }

    private org.springframework.test.web.servlet.ResultActions postEvent(String token, String path, Map<String, Object> body)
            throws Exception {
        if (body == null) {
            return mockMvc.perform(post(path).header("Authorization", "Bearer " + token));
        }
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    /* ----- DB 断言辅助 ----- */

    private Long userIdOf(String username) {
        return userDomainService.findUserByAccount(username).getId();
    }

    private String eventIdOf(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT event_id FROM ft_outbox_event WHERE aggregate_id = ? ORDER BY id DESC LIMIT 1",
                String.class, requestId);
    }

    private String orderIdOf(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM ft_order WHERE request_id = ?", String.class, requestId);
    }

    /** 查指定业务 ID 下最新一条匹配 eventType 的通知 Outbox 事件（未消费）。 */
    private NotificationEvent latestNotificationEvent(String bizId, String eventType) throws Exception {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT payload FROM ft_outbox_event WHERE topic = 'ft-notification' AND aggregate_id = ? ORDER BY id DESC",
                bizId);
        for (Map<String, Object> row : rows) {
            String payload = (String) row.get("payload");
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            if (eventType.equals(event.eventType())) {
                return event;
            }
        }
        return null;
    }
}
