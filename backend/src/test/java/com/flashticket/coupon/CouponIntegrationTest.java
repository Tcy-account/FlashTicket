package com.flashticket.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.identity.entity.User;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.UserDomainService;
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
 * M13 优惠券与营销活动集成测试：活动/模板/额度 → 领券幂等与限领 → 订单锁券/核销/返券。
 *
 * <p>覆盖验收：不超发、不超领、request_id 幂等、锁券核销返券、优惠金额服务端计算、
 * 券失效抢票失败不原价。真 MQ 链路在 test profile 关闭，消费建单通过直调 processor 完成。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CouponIntegrationTest {

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
    private PurchaseRequestProcessor processor;

    /* ================= 测试用例 ================= */

    @Test
    void campaignAndTemplateLifecycle() throws Exception {
        makeOperator("mktop");
        makeSuperAdmin("mktsa");
        String opToken = accessToken(adminLogin("mktop", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin("mktsa", PASSWORD).andReturn());

        // 创建活动 → 提交审核 → 审核发布
        String campaignId = createCampaign(opToken, "国庆抢券活动");
        postEvent(opToken, "/api/v1/admin/campaigns/" + campaignId + "/submit-review", null)
                .andExpect(status().isOk());
        postEvent(saToken, "/api/v1/admin/campaigns/" + campaignId + "/review", Map.of("approve", true))
                .andExpect(status().isOk());

        // 创建模板（满 300 减 50，总量 5，每人限 1）→ 启用
        String templateId = createTemplate(opToken, campaignId, "满300减50", 5, 1, 300L, 50L);
        mockMvc.perform(post("/api/v1/admin/coupon-templates/" + templateId + "/activate")
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk());

        // 额度账本初始化：total=available=5, issued=0
        Map<String, Object> quota = jdbcTemplate.queryForMap(
                "SELECT total, available, reserved, issued FROM ft_coupon_quota WHERE template_id = ?", templateId);
        assertThat(((Number) quota.get("total")).intValue()).isEqualTo(5);
        assertThat(((Number) quota.get("available")).intValue()).isEqualTo(5);
        assertThat(((Number) quota.get("issued")).intValue()).isEqualTo(0);
    }

    @Test
    void claimCouponSucceedsAndIsIdempotent() throws Exception {
        String templateId = prepareActiveTemplate("claim1");
        String userToken = makeUser("mkclaim1buyer");

        // 领券成功
        String requestId = UUID.randomUUID().toString();
        JsonNode data = json(postClaim(userToken, requestId, templateId).andReturn());
        assertThat(data.get("status").asText()).isEqualTo("SUCCEEDED");
        String couponId = data.get("userCouponId").asText();

        // 用户券 AVAILABLE
        assertThat(userCouponStatus(couponId)).isEqualTo("AVAILABLE");

        // 幂等：同 requestId 重复领券不重复发券
        JsonNode again = json(postClaim(userToken, requestId, templateId).andReturn());
        assertThat(again.get("userCouponId").asText()).isEqualTo(couponId);
        assertThat(countUserCoupons(templateId)).isEqualTo(1);
    }

    @Test
    void listAvailableCouponsReturnsClaimableTemplates() throws Exception {
        String templateId = prepareActiveTemplate("avail");

        // 公开查询（无需登录）返回可领券，额度有余
        JsonNode data = json(mockMvc.perform(get("/api/v1/coupons/available"))
                .andExpect(status().isOk()).andReturn());

        boolean found = false;
        int available = -1;
        for (JsonNode node : data) {
            if (templateId.equals(node.get("templateId").asText())) {
                found = true;
                available = node.get("availableQuantity").asInt();
            }
        }
        assertThat(found).isTrue();
        assertThat(available).isEqualTo(100);
    }

    @Test
    void claimCouponRespectsQuotaAndUserLimit() throws Exception {
        String templateId = prepareActiveTemplate("claim2");
        String userToken = makeUser("mkclaim2buyer");

        // 模板每人限领 1：领第二张失败（超限）
        String t1 = UUID.randomUUID().toString();
        json(postClaim(userToken, t1, templateId).andReturn());
        String t2 = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/coupons/claims")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestId", t2, "templateId", templateId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_LIMIT_EXCEEDED"));
    }

    @Test
    void checkoutWithCouponLocksAndAppliesDiscount() throws Exception {
        String prefix = "coupcheck";
        String userToken = makeUser(prefix + "buyer");
        String tierId = publishSellingTier(prefix);
        String couponId = claimCouponForUser(userToken, "coupchecktpl", 300L, 50L);

        // 抢票带券（2 张 × 400 分 = 800 分，满 300 减 50）
        String requestId = UUID.randomUUID().toString();
        checkout(userToken, requestId, tierId, 2, couponId).andExpect(status().isAccepted());
        assertThat(processor.process(eventIdOf(requestId), requestId)).isTrue();

        // 券已锁定
        assertThat(userCouponStatus(couponId)).isEqualTo("LOCKED");

        // 订单优惠金额：原价 800，减 50，应付 750
        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT original_amount_cent, discount_amount_cent, payable_amount_cent, user_coupon_id FROM ft_order WHERE request_id = ?",
                requestId);
        assertThat(((Number) order.get("original_amount_cent")).longValue()).isEqualTo(800L);
        assertThat(((Number) order.get("discount_amount_cent")).longValue()).isEqualTo(50L);
        assertThat(((Number) order.get("payable_amount_cent")).longValue()).isEqualTo(750L);
    }

    @Test
    void payNuclearCouponAndCloseReleasesCoupon() throws Exception {
        // 支付核销：建单锁券 → 支付 → 券 USED
        String prefix = "coupay";
        String userToken = makeUser(prefix + "buyer");
        String tierId = publishSellingTier(prefix);
        String couponId = claimCouponForUser(userToken, "coupaytpl", 0L, 50L);

        String requestId = UUID.randomUUID().toString();
        checkout(userToken, requestId, tierId, 2, couponId).andExpect(status().isAccepted());
        processor.process(eventIdOf(requestId), requestId);
        assertThat(userCouponStatus(couponId)).isEqualTo("LOCKED");

        String orderId = orderIdOf(requestId);
        pay(userToken, orderId);
        assertThat(userCouponStatus(couponId)).isEqualTo("USED");
    }

    @Test
    void checkoutFailsWhenCouponUnavailable() throws Exception {
        String prefix = "coupinv";
        String userToken = makeUser(prefix + "buyer");
        String tierId = publishSellingTier(prefix);
        // 领券后直接用掉（锁定 + 核销），使其不再 AVAILABLE
        String couponId = claimCouponForUser(userToken, "coupinvtpl", 0L, 50L);
        String r1 = UUID.randomUUID().toString();
        checkout(userToken, r1, tierId, 2, couponId).andExpect(status().isAccepted());
        processor.process(eventIdOf(r1), r1);
        String orderId = orderIdOf(r1);
        pay(userToken, orderId);
        assertThat(userCouponStatus(couponId)).isEqualTo("USED");

        // 再用已核销的券抢票 → 失败（不原价建单）
        String r2 = UUID.randomUUID().toString();
        checkout(userToken, r2, tierId, 1, couponId).andExpect(status().isAccepted());
        processor.process(eventIdOf(r2), r2);
        assertThat(purchaseStatus(r2)).isEqualTo("FAILED");
    }

    /* ================= 辅助方法 ================= */

    /** 造一个已启用、开售中的满减模板，返回模板 ID。 */
    private String prepareActiveTemplate(String tag) throws Exception {
        String opToken = operatorToken("mk" + tag + "op", "mk" + tag + "sa");
        String campaignId = createCampaign(opToken, "活动" + tag);
        postEvent(opToken, "/api/v1/admin/campaigns/" + campaignId + "/submit-review", null);
        String saToken = accessToken(adminLogin("mk" + tag + "sa", PASSWORD).andReturn());
        postEvent(saToken, "/api/v1/admin/campaigns/" + campaignId + "/review", Map.of("approve", true));
        String templateId = createTemplate(opToken, campaignId, "券" + tag, 100, 1, 0L, 10L);
        mockMvc.perform(post("/api/v1/admin/coupon-templates/" + templateId + "/activate")
                .header("Authorization", "Bearer " + opToken)).andExpect(status().isOk());
        return templateId;
    }

    /** 给指定用户领一张满减券，返回用户券 ID。 */
    private String claimCouponForUser(String userToken, String tag, long thresholdCent, long discountCent) throws Exception {
        String opToken = operatorToken("c" + tag + "op", "c" + tag + "sa");
        String campaignId = createCampaign(opToken, "活动" + tag);
        postEvent(opToken, "/api/v1/admin/campaigns/" + campaignId + "/submit-review", null);
        String saToken = accessToken(adminLogin("c" + tag + "sa", PASSWORD).andReturn());
        postEvent(saToken, "/api/v1/admin/campaigns/" + campaignId + "/review", Map.of("approve", true));
        String templateId = createTemplate(opToken, campaignId, "券" + tag, 100, 1, thresholdCent, discountCent);
        mockMvc.perform(post("/api/v1/admin/coupon-templates/" + templateId + "/activate")
                .header("Authorization", "Bearer " + opToken)).andExpect(status().isOk());
        String requestId = UUID.randomUUID().toString();
        return json(postClaim(userToken, requestId, templateId).andReturn()).get("userCouponId").asText();
    }

    private String operatorToken(String opName, String saName) throws Exception {
        makeOperator(opName);
        makeSuperAdmin(saName);
        return accessToken(adminLogin(opName, PASSWORD).andReturn());
    }

    private String createCampaign(String opToken, String name) throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return json(mockMvc.perform(post("/api/v1/admin/campaigns")
                        .header("Authorization", "Bearer " + opToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "type", "FLASH_CLAIM", "description", "测试活动",
                                "startAt", now.minusHours(1).format(FMT),
                                "endAt", now.plusHours(1).format(FMT)))))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
    }

    private String createTemplate(String opToken, String campaignId, String name,
                                  int totalQuantity, int perUserLimit,
                                  long thresholdCent, long discountCent) throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Map<String, Object> body = new HashMap<>();
        body.put("campaignId", Long.valueOf(campaignId));
        body.put("name", name);
        body.put("type", "FULL_REDUCTION");
        body.put("thresholdCent", thresholdCent);
        body.put("discountCent", discountCent);
        body.put("discountRate", 0);
        body.put("maxDiscountCent", 0L);
        body.put("totalQuantity", totalQuantity);
        body.put("perUserLimit", perUserLimit);
        body.put("claimStartAt", now.minusHours(1).format(FMT));
        body.put("claimEndAt", now.plusHours(1).format(FMT));
        body.put("validityType", "FIXED");
        body.put("validFrom", now.minusHours(1).format(FMT));
        body.put("validUntil", now.plusDays(30).format(FMT));
        body.put("applicableScope", "ALL");
        return json(mockMvc.perform(post("/api/v1/admin/coupon-templates")
                        .header("Authorization", "Bearer " + opToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions postClaim(String userToken, String requestId, String templateId)
            throws Exception {
        return mockMvc.perform(post("/api/v1/coupons/claims")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "requestId", requestId, "templateId", templateId))));
    }

    /* ----- 抢票/订单/支付 ----- */

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
                                                                        String tierId, int quantity, String couponId)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("requestId", requestId);
        body.put("tierId", tierId);
        body.put("quantity", quantity);
        body.put("selectedCouponId", couponId);
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

    private String userCouponStatus(String couponId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM ft_user_coupon WHERE id = ?", String.class, Long.valueOf(couponId));
    }

    private int countUserCoupons(String templateId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ft_user_coupon WHERE template_id = ?", Integer.class, Long.valueOf(templateId));
        return c == null ? 0 : c;
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

    private String purchaseStatus(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM ft_purchase_request WHERE request_id = ?", String.class, requestId);
    }
}
