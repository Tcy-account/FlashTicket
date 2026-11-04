package com.flashticket.risk;

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
 * M15-B 风控限制集成测试：临时限制阻断抢票/抢券、解除、业务范围隔离、规则生命周期。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RiskRestrictionIntegrationTest {

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

    /* ================= 测试用例 ================= */

    @Test
    void restrictionBlocksPurchaseAndRevokeRecovers() throws Exception {
        String prefix = "rlrestrict";
        String userToken = makeUser(prefix + "buyer");
        Long userId = userIdOf(prefix + "buyer");
        String tierId = publishSellingTier(prefix);
        // sa 已在 publishSellingTier 内注册，这里仅登录取 token（避免重复注册触发验证码限频）
        String saToken = accessToken(adminLogin(prefix + "sa", PASSWORD).andReturn());

        // 创建 PURCHASE 限制
        String restrictionId = createRestriction(saToken, "USER", String.valueOf(userId), "PURCHASE");

        // 被限制用户抢票 → 403 RISK_RESTRICTED
        checkout(userToken, UUID.randomUUID().toString(), tierId, 1)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RISK_RESTRICTED"));

        // 解除限制后恢复
        mockMvc.perform(post("/api/v1/admin/risk/restrictions/" + restrictionId + "/revoke")
                        .header("Authorization", "Bearer " + saToken))
                .andExpect(status().isOk());
        checkout(userToken, UUID.randomUUID().toString(), tierId, 1).andExpect(status().isAccepted());
    }

    @Test
    void restrictionIsScopedByBusiness() throws Exception {
        String prefix = "rlscope";
        String userToken = makeUser(prefix + "buyer");
        Long userId = userIdOf(prefix + "buyer");
        String tierId = publishSellingTier(prefix);
        String saToken = accessToken(adminLogin(prefix + "sa", PASSWORD).andReturn());

        // 仅限制领券（CLAIM），不限制抢票
        createRestriction(saToken, "USER", String.valueOf(userId), "CLAIM");

        // 抢票放行（scope 隔离）
        checkout(userToken, UUID.randomUUID().toString(), tierId, 1).andExpect(status().isAccepted());
    }

    @Test
    void ruleLifecycle() throws Exception {
        String saToken = superAdminToken("rlrulesa");

        // 创建规则
        JsonNode created = json(mockMvc.perform(post("/api/v1/admin/risk/rules")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ruleCode", "RL_TEST_" + System.currentTimeMillis(),
                                "name", "测试规则", "scene", "PURCHASE", "action", "THROTTLE",
                                "config", "{}", "priority", 1))))
                .andExpect(status().isOk()).andReturn());
        String ruleId = created.get("id").asText();

        // 列表包含该规则
        JsonNode list = json(mockMvc.perform(get("/api/v1/admin/risk/rules")
                .header("Authorization", "Bearer " + saToken)).andExpect(status().isOk()).andReturn());
        assertThat(list.size()).isGreaterThanOrEqualTo(1);

        // 停用规则
        mockMvc.perform(post("/api/v1/admin/risk/rules/" + ruleId + "/toggle")
                        .header("Authorization", "Bearer " + saToken)
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        // 审计记录可查
        JsonNode ops = json(mockMvc.perform(get("/api/v1/admin/risk/operations")
                .header("Authorization", "Bearer " + saToken)).andExpect(status().isOk()).andReturn());
        assertThat(ops.size()).isGreaterThanOrEqualTo(1);
    }

    /* ================= 辅助方法 ================= */

    private String createRestriction(String saToken, String subjectType, String subjectId, String scope) throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return json(mockMvc.perform(post("/api/v1/admin/risk/restrictions")
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subjectType", subjectType, "subjectId", subjectId, "scope", scope,
                                "reason", "测试限制", "expireAt", now.plusHours(1).format(FMT)))))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
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

    /* ----- 身份与通用 ----- */

    private String operatorToken(String opName, String saName) throws Exception {
        makeOperator(opName);
        makeSuperAdmin(saName);
        return accessToken(adminLogin(opName, PASSWORD).andReturn());
    }

    private String superAdminToken(String username) throws Exception {
        makeSuperAdmin(username);
        return accessToken(adminLogin(username, PASSWORD).andReturn());
    }

    private String makeUser(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        return userToken(username, PASSWORD);
    }

    private Long userIdOf(String username) {
        return userDomainService.findUserByAccount(username).getId();
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
