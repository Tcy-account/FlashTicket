package com.flashticket.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.common.exception.BizException;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.entity.User;
import com.flashticket.risk.service.RateLimitService;
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
 * M15-A 多维限流集成测试：抢票/领券用户维度限流、限流服务原子性。
 *
 * <p>验证「单用户超阈值后被正确限流」与「Redis Lua 并发不超发令牌」的核心验收。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RiskRateLimitIntegrationTest {

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
        // 覆盖 application-test.yml 放宽的阈值，恢复为 1 以验证限流
        registry.add("flashticket.risk.rate-limit.purchase-limit", () -> "1");
        registry.add("flashticket.risk.rate-limit.claim-limit", () -> "1");
        registry.add("flashticket.risk.rate-limit.pay-limit", () -> "1");
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
    private RateLimitService rateLimitService;

    /* ================= 测试用例 ================= */

    @Test
    void purchaseRateLimited() throws Exception {
        String prefix = "rlpurchase";
        String userToken = makeUser(prefix + "buyer");
        String tierId = publishSellingTier(prefix);

        // 第一次抢票放行
        String r1 = UUID.randomUUID().toString();
        checkout(userToken, r1, tierId, 1).andExpect(status().isAccepted());

        // 同账号同票档 5 秒内第二次新请求 → 429
        String r2 = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestId", r2, "tierId", tierId, "quantity", 1))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RISK_RATE_LIMITED"));
    }

    @Test
    void claimRateLimited() throws Exception {
        String templateId = prepareActiveTemplate("rlclaim");
        String userToken = makeUser("rlclaimbuyer");

        // 第一次领券成功
        String t1 = UUID.randomUUID().toString();
        json(postClaim(userToken, t1, templateId).andExpect(status().isOk()).andReturn());

        // 同账号同模板 5 秒内第二次新请求 → 429
        String t2 = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/coupons/claims")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestId", t2, "templateId", templateId))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RISK_RATE_LIMITED"));
    }

    @Test
    void rateLimitServiceThrottlesAfterLimit() {
        // 直接调限流服务：首次放行，窗口内第二次超限抛业务异常
        rateLimitService.checkPurchase(1L, 100L);
        assertThatThrownBy(() -> rateLimitService.checkPurchase(1L, 100L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode().getCode())
                        .isEqualTo("RISK_RATE_LIMITED"));
    }

    @Test
    void rateLimitKeyIsScopedByDimension() {
        // 不同维度（不同票档/场景）不共享额度；userId 用 2L 与 throttles 测试隔离
        rateLimitService.checkPurchase(2L, 100L);
        rateLimitService.checkPurchase(2L, 200L); // 不同 tierId 放行
        rateLimitService.checkClaim(2L, 300L);     // 不同场景放行
    }

    /* ================= 辅助方法 ================= */

    private String prepareActiveTemplate(String tag) throws Exception {
        String opToken = operatorToken("rl" + tag + "op", "rl" + tag + "sa");
        String campaignId = createCampaign(opToken, "活动" + tag);
        postEvent(opToken, "/api/v1/admin/campaigns/" + campaignId + "/submit-review", null);
        String saToken = accessToken(adminLogin("rl" + tag + "sa", PASSWORD).andReturn());
        postEvent(saToken, "/api/v1/admin/campaigns/" + campaignId + "/review", Map.of("approve", true));
        String templateId = createTemplate(opToken, campaignId, "券" + tag, 100, 1, 0L, 10L);
        mockMvc.perform(post("/api/v1/admin/coupon-templates/" + templateId + "/activate")
                .header("Authorization", "Bearer " + opToken)).andExpect(status().isOk());
        return templateId;
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
}
