package com.flashticket.purchase;

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
import com.flashticket.purchase.service.PurchaseCheckoutService;
import com.flashticket.purchase.service.PurchaseRecoveryService;
import com.flashticket.purchase.service.PurchaseRequestProcessor;
import com.flashticket.purchase.service.PurchaseQueryService;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * M12 异步抢票与可靠消息集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：可靠受理（202 + QUEUED + Outbox 落库）、消费端建单（直接调
 * {@link PurchaseRequestProcessor}）、消息重复不重复建单（Inbox + requestId 三重
 * 幂等）、受理预检拒绝（未开售/暂停/数量非法）、消费端确定性失败（售罄/限购）、
 * 恢复（租约过期复位 / RECOVERING 补发 / 排队超时终态）、取消排队、状态查询、
 * 管理端查询与重试权限。
 *
 * <p>test profile 下 MQ listener/publisher/scheduler 均不加载，消费正确性通过
 * 直接调用核心处理器验证（见 doc/抢票请求与异步排队模块.md 十二 验收）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PurchaseCheckoutIntegrationTest {

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
    private PurchaseRecoveryService recoveryService;

    // ===== 受理与建单 =====

    @Test
    void acceptReturnsQueuedAndPersistsOutbox() throws Exception {
        String tierId = publishSellingTier("acc", 100);
        String token = makeUser("buyeracc");

        checkout(token, "acc-req", tierId, 2)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.order").doesNotExist())
                .andReturn();

        // 请求落 QUEUED + Outbox 落 PENDING（可靠受理）
        String reqStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM ft_purchase_request WHERE request_id = 'acc-req'", String.class);
        assertThat(reqStatus).isEqualTo("QUEUED");
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_outbox_event WHERE aggregate_id = 'acc-req' AND status = 'PENDING'",
                Integer.class);
        assertThat(outboxCount).isEqualTo(1);
    }

    @Test
    void processorCreatesOrderAndConsumesInventory() throws Exception {
        String tierId = publishSellingTier("proc", 100);
        makeUser("buyerproc");
        String token = userToken("buyerproc", PASSWORD);

        checkout(token, "proc-req", tierId, 2).andExpect(status().isAccepted());
        String eventId = eventIdOf("proc-req");

        assertThat(processor.process(eventId, "proc-req")).isTrue();

        // 请求终态 SUCCEEDED
        String reqStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM ft_purchase_request WHERE request_id = 'proc-req'", String.class);
        assertThat(reqStatus).isEqualTo("SUCCEEDED");

        // 订单创建 + 明细快照
        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT id, status, payable_amount_cent FROM ft_order WHERE request_id = 'proc-req'");
        assertThat(order.get("status")).isEqualTo("PENDING_PAYMENT");
        assertThat(((Number) order.get("payable_amount_cent")).longValue()).isEqualTo(200L);
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_order_item WHERE order_id = ?", Integer.class,
                ((Number) order.get("id")).longValue());
        assertThat(itemCount).isEqualTo(1);

        // 库存预占 + 限购占用
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);
        Long userId = userIdOf("buyerproc");
        assertLimit(userId, Long.valueOf(tierId), 2, 0);

        // Inbox 落库（消费幂等记录）
        Integer inboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_message_inbox WHERE event_id = ?", Integer.class, eventId);
        assertThat(inboxCount).isEqualTo(1);
    }

    // ===== 消息重复幂等 =====

    @Test
    void duplicateEventIsIdempotent() throws Exception {
        String tierId = publishSellingTier("dupevt", 100);
        makeUser("buyerdupevt");
        String token = userToken("buyerdupevt", PASSWORD);

        checkout(token, "dupevt-req", tierId, 2).andExpect(status().isAccepted());
        String eventId = eventIdOf("dupevt-req");

        assertThat(processor.process(eventId, "dupevt-req")).isTrue();
        // 同 eventId 重复投递：Inbox 幂等，直接跳过，不重复建单
        assertThat(processor.process(eventId, "dupevt-req")).isTrue();

        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_order WHERE request_id = 'dupevt-req'", Integer.class);
        assertThat(orderCount).isEqualTo(1);
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);
    }

    @Test
    void duplicateRequestIdAcrossEventsCreatesOneOrder() throws Exception {
        String tierId = publishSellingTier("dupreq", 100);
        makeUser("buyerdupreq");
        String token = userToken("buyerdupreq", PASSWORD);

        checkout(token, "dupreq-id", tierId, 2).andExpect(status().isAccepted());
        String firstEventId = eventIdOf("dupreq-id");

        assertThat(processor.process(firstEventId, "dupreq-id")).isTrue();
        // 另一条消息（不同 eventId）携带同一 requestId：请求已终态，幂等跳过
        assertThat(processor.process("second-event-id", "dupreq-id")).isTrue();

        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_order WHERE request_id = 'dupreq-id'", Integer.class);
        assertThat(orderCount).isEqualTo(1);
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);
    }

    // ===== 受理预检（快速拒绝） =====

    @Test
    void acceptRejectsWindowNotOpen() throws Exception {
        String tierId = publishSellingTierWithWindow("win", 100, futureWindow());
        String token = makeUser("buyerwin");

        checkout(token, "win-req", tierId, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PURCHASE_WINDOW_NOT_OPEN"));
    }

    @Test
    void acceptRejectsPausedSale() throws Exception {
        String tierId = publishSellingTier("pause", 100);
        String opToken = accessToken(adminLogin("pauseop", PASSWORD).andReturn());
        String token = makeUser("buyerpause");

        mockMvc.perform(post("/api/v1/admin/tiers/{id}/pause", tierId)
                        .header("Authorization", "Bearer " + opToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "测试暂停"))))
                .andExpect(status().isOk());

        checkout(token, "pause-req", tierId, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PURCHASE_SALE_PAUSED"));
    }

    @Test
    void acceptRejectsInvalidQuantity() throws Exception {
        String tierId = publishSellingTier("qty", 100);
        String token = makeUser("buyerqty");

        checkout(token, "qty-req", tierId, 7)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVENTORY_QUANTITY_INVALID"));
    }

    // ===== 消费端确定性失败 =====

    @Test
    void processorMarksFailedOnSoldOut() throws Exception {
        String tierId = publishSellingTier("soldout", 3);
        makeUser("buyera");
        makeUser("buyerb");
        String tokenA = userToken("buyera", PASSWORD);
        String tokenB = userToken("buyerb", PASSWORD);

        // A 受理并处理成功（抢 2，剩 1）
        checkout(tokenA, "so-a", tierId, 2).andExpect(status().isAccepted());
        assertThat(processor.process(eventIdOf("so-a"), "so-a")).isTrue();

        // B 受理后处理：售罄 → FAILED
        checkout(tokenB, "so-b", tierId, 2).andExpect(status().isAccepted());
        assertThat(processor.process(eventIdOf("so-b"), "so-b")).isTrue();

        String statusB = jdbcTemplate.queryForObject(
                "SELECT status FROM ft_purchase_request WHERE request_id = 'so-b'", String.class);
        assertThat(statusB).isEqualTo("FAILED");
        String failReason = jdbcTemplate.queryForObject(
                "SELECT fail_reason FROM ft_purchase_request WHERE request_id = 'so-b'", String.class);
        assertThat(failReason).isEqualTo("INVENTORY_SOLD_OUT");
        assertStock(Long.valueOf(tierId), 3, 1, 2, 0);
    }

    @Test
    void processorMarksFailedOnLimitExceeded() throws Exception {
        String tierId = publishSellingTier("lim", 100);
        makeUser("buyerlim");
        String token = userToken("buyerlim", PASSWORD);

        // perUserLimit=2，先买 2 张用满名额
        checkout(token, "lim-1", tierId, 2).andExpect(status().isAccepted());
        assertThat(processor.process(eventIdOf("lim-1"), "lim-1")).isTrue();

        // 再买 1 张超限：受理成功（余票足），消费端限购占用失败 → FAILED
        checkout(token, "lim-2", tierId, 1).andExpect(status().isAccepted());
        assertThat(processor.process(eventIdOf("lim-2"), "lim-2")).isTrue();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM ft_purchase_request WHERE request_id = 'lim-2'", String.class);
        assertThat(status).isEqualTo("FAILED");
        String failReason = jdbcTemplate.queryForObject(
                "SELECT fail_reason FROM ft_purchase_request WHERE request_id = 'lim-2'", String.class);
        assertThat(failReason).isEqualTo("INVENTORY_LIMIT_EXCEEDED");
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);
    }

    // ===== 恢复 =====

    @Test
    void recoverRepublishesRecoveringRequest() throws Exception {
        String tierId = publishSellingTier("recover", 100);
        makeUser("buyerrecover");
        String token = userToken("buyerrecover", PASSWORD);

        checkout(token, "recover-req", tierId, 2).andExpect(status().isAccepted());

        // 模拟技术故障：请求进入 RECOVERING 且到重试时间
        jdbcTemplate.update("UPDATE ft_purchase_request SET status = 'RECOVERING', " +
                        "next_retry_at = ? WHERE request_id = 'recover-req'",
                LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));

        // 恢复补发新事件
        assertThat(recoveryService.recover("recover-req")).isTrue();
        String newEventId = latestEventIdOf("recover-req");

        // 新事件处理成功
        assertThat(processor.process(newEventId, "recover-req")).isTrue();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM ft_purchase_request WHERE request_id = 'recover-req'", String.class);
        assertThat(status).isEqualTo("SUCCEEDED");
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);
    }

    @Test
    void recoverLeaseExpiredResetsProcessing() throws Exception {
        String tierId = publishSellingTier("lease", 100);
        makeUser("buyerlease");
        String token = userToken("buyerlease", PASSWORD);

        checkout(token, "lease-req", tierId, 2).andExpect(status().isAccepted());

        // 模拟消费者崩溃：卡在 PROCESSING 且租约已过期
        jdbcTemplate.update("UPDATE ft_purchase_request SET status = 'PROCESSING', " +
                        "lease_owner = 'dead-consumer', lease_until = ? WHERE request_id = 'lease-req'",
                LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));

        // 租约过期复位 + 补发
        assertThat(recoveryService.recoverLeaseExpired("lease-req")).isTrue();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM ft_purchase_request WHERE request_id = 'lease-req'", String.class);
        assertThat(status).isEqualTo("RECOVERING");

        // 新事件处理成功
        String newEventId = latestEventIdOf("lease-req");
        assertThat(processor.process(newEventId, "lease-req")).isTrue();
        assertStock(Long.valueOf(tierId), 100, 98, 2, 0);
    }

    @Test
    void queueTimeoutMarksFailed() throws Exception {
        String tierId = publishSellingTier("qtimeout", 100);
        makeUser("buyerqtimeout");
        String token = userToken("buyerqtimeout", PASSWORD);

        checkout(token, "qtimeout-req", tierId, 2).andExpect(status().isAccepted());

        // 模拟排队超时：QUEUED 但已过受理截止时间
        jdbcTemplate.update("UPDATE ft_purchase_request SET accepted_deadline_at = ? " +
                        "WHERE request_id = 'qtimeout-req'",
                LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));

        recoveryService.failQueueTimeout("qtimeout-req");

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM ft_purchase_request WHERE request_id = 'qtimeout-req'", String.class);
        assertThat(status).isEqualTo("FAILED");
        String failReason = jdbcTemplate.queryForObject(
                "SELECT fail_reason FROM ft_purchase_request WHERE request_id = 'qtimeout-req'", String.class);
        assertThat(failReason).isEqualTo("QUEUE_TIMEOUT");
        // 未创建订单、库存未动
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_order WHERE request_id = 'qtimeout-req'", Integer.class);
        assertThat(orderCount).isZero();
        assertStock(Long.valueOf(tierId), 100, 100, 0, 0);
    }

    // ===== 取消与查询 =====

    @Test
    void cancelQueuedRequest() throws Exception {
        String tierId = publishSellingTier("cancel", 100);
        makeUser("buyercancel");
        String token = userToken("buyercancel", PASSWORD);

        checkout(token, "cancel-req", tierId, 2).andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/purchases/{requestId}/cancel", "cancel-req")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // 已取消请求不再被处理（幂等跳过）
        assertThat(processor.process(eventIdOf("cancel-req"), "cancel-req")).isTrue();
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_order WHERE request_id = 'cancel-req'", Integer.class);
        assertThat(orderCount).isZero();
    }

    @Test
    void getStatusReturnsCurrentStatus() throws Exception {
        String tierId = publishSellingTier("status", 100);
        makeUser("buyerstatus");
        String token = userToken("buyerstatus", PASSWORD);

        checkout(token, "status-req", tierId, 2).andExpect(status().isAccepted());
        assertThat(processor.process(eventIdOf("status-req"), "status-req")).isTrue();

        mockMvc.perform(get("/api/v1/purchases/{requestId}", "status-req")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.order.status").value("PENDING_PAYMENT"));
    }

    @Test
    void listMyPurchasesReturnsOnlyOwn() throws Exception {
        String tierId = publishSellingTier("listp", 100);
        makeUser("buyerlistp1");
        makeUser("buyerlistp2");
        String tokenA = userToken("buyerlistp1", PASSWORD);
        String tokenB = userToken("buyerlistp2", PASSWORD);

        checkout(tokenA, "listp-a", tierId, 1).andExpect(status().isAccepted());
        checkout(tokenB, "listp-b", tierId, 1).andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/purchases")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].requestId").value("listp-a"));
    }

    // ===== 订单查询与权限（M08 语义延续，M12 异步建单后查询） =====

    @Test
    void orderAccessControlPreventsCrossUserAccess() throws Exception {
        String tierId = publishSellingTier("acl", 100);
        makeUser("buyeracl1");
        makeUser("buyeracl2");
        String tokenA = userToken("buyeracl1", PASSWORD);
        String tokenB = userToken("buyeracl2", PASSWORD);

        String orderId = completePurchase(tokenA, "acl-req", tierId, 1);

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_OWNED"));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId));
    }

    @Test
    void listMyOrdersReturnsOnlyOwnOrders() throws Exception {
        String tierId = publishSellingTier("listo", 100);
        makeUser("buyerlisto1");
        makeUser("buyerlisto2");
        String tokenA = userToken("buyerlisto1", PASSWORD);
        String tokenB = userToken("buyerlisto2", PASSWORD);

        completePurchase(tokenA, "listo-a", tierId, 1);
        completePurchase(tokenB, "listo-b", tierId, 1);

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].requestId").value("listo-a"));
    }

    @Test
    void adminOrderQueryRequiresPermission() throws Exception {
        String tierId = publishSellingTier("permorder", 100);
        makeUser("buyerpermorder");
        String token = userToken("buyerpermorder", PASSWORD);
        completePurchase(token, "permorder-req", tierId, 1);

        makeAdmin("ordreadadmin");
        makeOperator("ordreadop");
        String adminToken = accessToken(adminLogin("ordreadadmin", PASSWORD).andReturn());
        String opToken = accessToken(adminLogin("ordreadop", PASSWORD).andReturn());

        // ADMIN 无 order:read
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        // OPERATOR 有 order:read
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk());
    }

    // ===== 管理端查询与重试权限 =====

    @Test
    void adminPurchaseQueryAndRetryRequiresPermission() throws Exception {
        String tierId = publishSellingTier("perm", 100);
        makeUser("buyerperm");
        String token = userToken("buyerperm", PASSWORD);
        checkout(token, "perm-req", tierId, 1).andExpect(status().isAccepted());

        makeAdmin("purreadadmin");
        makeOperator("purreadop");
        makeSuperAdmin("purreadsa");
        String adminToken = accessToken(adminLogin("purreadadmin", PASSWORD).andReturn());
        String opToken = accessToken(adminLogin("purreadop", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin("purreadsa", PASSWORD).andReturn());

        // ADMIN 无 purchase:read
        mockMvc.perform(get("/api/v1/admin/purchases")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        // OPERATOR 有 purchase:read
        mockMvc.perform(get("/api/v1/admin/purchases")
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk());

        // 构造可恢复请求后，OPERATOR 无 purchase:retry
        jdbcTemplate.update("UPDATE ft_purchase_request SET status = 'RECOVERING' " +
                "WHERE request_id = 'perm-req'");
        mockMvc.perform(post("/api/v1/admin/purchases/{requestId}/retry", "perm-req")
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isForbidden());

        // SUPER_ADMIN 有 purchase:retry
        mockMvc.perform(post("/api/v1/admin/purchases/{requestId}/retry", "perm-req")
                        .header("Authorization", "Bearer " + saToken))
                .andExpect(status().isOk());
    }

    // ===== helpers =====

    private ResultActions checkout(String token, String requestId, String tierId, int quantity) throws Exception {
        return mockMvc.perform(post("/api/v1/purchases")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "requestId", requestId, "tierId", tierId, "quantity", quantity))));
    }

    /** 受理事件后直接消费建单，返回订单 ID（供订单查询类测试复用）。 */
    private String completePurchase(String token, String requestId, String tierId, int quantity) throws Exception {
        checkout(token, requestId, tierId, quantity).andExpect(status().isAccepted());
        assertThat(processor.process(eventIdOf(requestId), requestId)).isTrue();
        return jdbcTemplate.queryForObject(
                "SELECT id FROM ft_order WHERE request_id = ?", String.class, requestId);
    }

    /** 受理事件的 eventId（首次受理时写入 Outbox 的 event_id）。 */
    private String eventIdOf(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT event_id FROM ft_outbox_event WHERE aggregate_id = ? ORDER BY id ASC LIMIT 1",
                String.class, requestId);
    }

    /** 最近一次补发的事件 eventId（恢复补发后 Outbox 有多条，取最新）。 */
    private String latestEventIdOf(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT event_id FROM ft_outbox_event WHERE aggregate_id = ? ORDER BY id DESC LIMIT 1",
                String.class, requestId);
    }

    /** 发布一个「开售中」的演出，返回票档 ID（售票窗口 now-1h ~ now+1h）。 */
    private String publishSellingTier(String prefix, int quantity) throws Exception {
        return publishSellingTierWithWindow(prefix, quantity, sellingWindow());
    }

    private String publishSellingTierWithWindow(String prefix, int quantity, String[] window) throws Exception {
        makeOperator(prefix + "op");
        makeSuperAdmin(prefix + "sa");
        String opToken = accessToken(adminLogin(prefix + "op", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin(prefix + "sa", PASSWORD).andReturn());
        String eventId = createFullEvent(opToken, "抢票测试活动" + prefix);
        String venueId = createVenue(opToken, "抢票场馆" + prefix, "北京", 5000);
        String sessionId = createSession(opToken, eventId, venueId, "晚场",
                "2027-05-01T19:00:00", "2027-05-01T21:00:00", 2000);
        String tierId = createTier(opToken, sessionId, "普通票", 100L, quantity);
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

    private String[] futureWindow() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return new String[]{now.plusHours(1).format(FMT), now.plusHours(2).format(FMT)};
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

    private String makeUser(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        return userToken(username, PASSWORD);
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

    private String accessToken(MvcResult loginResult) throws Exception {
        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
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
        body.put("detailContent", "演出详情介绍");
        body.put("organizerName", "主办方文化公司");
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
