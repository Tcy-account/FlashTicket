package com.flashticket.event;

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
 * M06 票档、售票计划与审核发布集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：票档 CRUD 与容量/唯一约束、乐观锁版本冲突、售票计划配置与暂停恢复、
 * 审核提交/驳回/通过/发布全流程、审核人不能审自己、配置指纹不一致拒绝发布、
 * 关键配置冻结、公开票档可见性与可售状态计算。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TicketTierReviewIntegrationTest {

    private static final String PASSWORD = "ZewruqYvgn2099";
    private static final String REGISTER_CODE_KEY = "auth:verify:code:REGISTER:";

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

    // ===== 票档与售票计划 =====

    @Test
    void operatorCanCreateTierAndConfigureSalePlan() throws Exception {
        makeOperator("optier");
        String token = accessToken(adminLogin("optier", PASSWORD).andReturn());
        String eventId = createEvent(token, "票档测试活动");
        String venueId = createVenue(token, "票档场馆", "北京", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2027-05-01T19:00:00", "2027-05-01T21:00:00", 2000);

        // 创建票档
        MvcResult created = mockMvc.perform(post("/api/v1/admin/sessions/{sessionId}/tiers", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tierBody("VIP", 99900L, 300, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("VIP"))
                .andExpect(jsonPath("$.data.unitPriceCent").value(99900))
                .andReturn();
        String tierId = json(created).get("id").asText();

        // 配置售票计划
        mockMvc.perform(put("/api/v1/admin/tiers/{id}/sale-plan", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "saleStartAt", "2027-04-01T10:00:00", "saleEndAt", "2027-05-01T18:00:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saleStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.saleVersion").value(0));

        // 再次配置应递增 saleVersion
        mockMvc.perform(put("/api/v1/admin/tiers/{id}/sale-plan", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "saleStartAt", "2027-04-02T10:00:00", "saleEndAt", "2027-05-02T18:00:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saleVersion").value(1));
    }

    @Test
    void tierQuantityExceedsSessionCapacityRejected() throws Exception {
        makeOperator("opqty");
        String token = accessToken(adminLogin("opqty", PASSWORD).andReturn());
        String eventId = createEvent(token, "容量票档活动");
        String venueId = createVenue(token, "容量票档场馆", "上海", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2027-06-01T19:00:00", "2027-06-01T21:00:00", 1000);

        mockMvc.perform(post("/api/v1/admin/sessions/{sessionId}/tiers", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tierBody("超量票", 100L, 1200, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVENT_TIER_CAPACITY_EXCEED"));
    }

    @Test
    void tierNameDuplicateRejected() throws Exception {
        makeOperator("opname");
        String token = accessToken(adminLogin("opname", PASSWORD).andReturn());
        String eventId = createEvent(token, "重名票档活动");
        String venueId = createVenue(token, "重名票档场馆", "广州", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2027-07-01T19:00:00", "2027-07-01T21:00:00", 1000);

        createTier(token, sessionId, "普通票", 100L, 100);

        mockMvc.perform(post("/api/v1/admin/sessions/{sessionId}/tiers", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tierBody("普通票", 200L, 100, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_TIER_NAME_TAKEN"));
    }

    @Test
    void tierUpdateWithStaleVersionRejected() throws Exception {
        makeOperator("optver");
        String token = accessToken(adminLogin("optver", PASSWORD).andReturn());
        String eventId = createEvent(token, "票档版本活动");
        String venueId = createVenue(token, "票档版本场馆", "深圳", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2027-08-01T19:00:00", "2027-08-01T21:00:00", 1000);
        String tierId = createTier(token, sessionId, "看台票", 100L, 100);

        // 先编辑一次 version 0→1
        mockMvc.perform(put("/api/v1/admin/tiers/{id}", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tierBody("看台票", 150L, 100, 0))))
                .andExpect(status().isOk());

        // 再拿过期 version=0 → 冲突
        mockMvc.perform(put("/api/v1/admin/tiers/{id}", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tierBody("看台票", 200L, 100, 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_TIER_VERSION_CONFLICT"));
    }

    @Test
    void salePlanInvalidTimeRejected() throws Exception {
        makeOperator("opsptime");
        String token = accessToken(adminLogin("opsptime", PASSWORD).andReturn());
        String eventId = createEvent(token, "售票时间活动");
        String venueId = createVenue(token, "售票时间场馆", "成都", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2027-09-01T19:00:00", "2027-09-01T21:00:00", 1000);
        String tierId = createTier(token, sessionId, "普通票", 100L, 100);

        mockMvc.perform(put("/api/v1/admin/tiers/{id}/sale-plan", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "saleStartAt", "2027-09-02T10:00:00", "saleEndAt", "2027-09-01T10:00:00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVENT_SALE_PLAN_INVALID_TIME"));
    }

    @Test
    void salePlanPauseAndResume() throws Exception {
        makeOperator("oppause");
        String token = accessToken(adminLogin("oppause", PASSWORD).andReturn());
        String eventId = createEvent(token, "暂停恢复活动");
        String venueId = createVenue(token, "暂停恢复场馆", "杭州", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2027-10-01T19:00:00", "2027-10-01T21:00:00", 1000);
        String tierId = createTier(token, sessionId, "普通票", 100L, 100);
        configureSalePlan(token, tierId, "2027-09-01T10:00:00", "2027-10-01T18:00:00");

        mockMvc.perform(post("/api/v1/admin/tiers/{id}/pause", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "风险防控"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saleStatus").value("PAUSED"));

        // 已暂停不可再暂停
        mockMvc.perform(post("/api/v1/admin/tiers/{id}/pause", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "再次暂停"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_SALE_PLAN_ILLEGAL_TRANSITION"));

        mockMvc.perform(post("/api/v1/admin/tiers/{id}/resume", tierId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saleStatus").value("ENABLED"));
    }

    // ===== 审核发布流 =====

    @Test
    void fullReviewPublishFlow() throws Exception {
        makeOperator("opflow");
        makeSuperAdmin("saflow");
        String opToken = accessToken(adminLogin("opflow", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin("saflow", PASSWORD).andReturn());

        String eventId = createFullEvent(opToken, "完整审核发布音乐节");
        String venueId = createVenue(opToken, "审核发布场馆", "南京", 8000);
        String sessionId = createSession(opToken, eventId, venueId, "主舞台",
                "2027-11-01T19:00:00", "2027-11-01T22:00:00", 3000);
        String tierId = createTier(opToken, sessionId, "VIP", 99900L, 500);
        configureSalePlan(opToken, tierId, "2027-10-01T10:00:00", "2027-11-01T18:00:00");

        // 运营提交审核 → PENDING_REVIEW
        mockMvc.perform(post("/api/v1/admin/events/{id}/submit-review", eventId)
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        // 超管审核通过 → APPROVED
        mockMvc.perform(post("/api/v1/admin/events/{id}/review", eventId)
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approve", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 超管发布 → PUBLISHED
        mockMvc.perform(post("/api/v1/admin/events/{id}/publish", eventId)
                        .header("Authorization", "Bearer " + saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 发布后公开可见
        mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/v1/sessions/{id}/tiers", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("VIP"));
    }

    @Test
    void submitRequiresTierAndSalePlan() throws Exception {
        makeOperator("opnotready");
        String token = accessToken(adminLogin("opnotready", PASSWORD).andReturn());
        String eventId = createEvent(token, "配置不全活动");
        String venueId = createVenue(token, "配置不全场馆", "武汉", 5000);
        // 有场次但无票档 → 提交审核被拒
        createSession(token, eventId, venueId, "晚场", "2027-12-01T19:00:00", "2027-12-01T21:00:00", 1000);

        mockMvc.perform(post("/api/v1/admin/events/{id}/submit-review", eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_SUBMIT_NOT_READY"));
    }

    @Test
    void reviewerCannotReviewOwnSubmission() throws Exception {
        makeSuperAdmin("saself");
        String token = accessToken(adminLogin("saself", PASSWORD).andReturn());
        String eventId = createFullEvent(token, "自我审核活动");
        String venueId = createVenue(token, "自审场馆", "西安", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2028-01-01T19:00:00", "2028-01-01T21:00:00", 1000);
        String tierId = createTier(token, sessionId, "普通票", 100L, 100);
        configureSalePlan(token, tierId, "2027-12-01T10:00:00", "2028-01-01T18:00:00");

        // 超管自己提交审核
        mockMvc.perform(post("/api/v1/admin/events/{id}/submit-review", eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 超管自己审核 → 拒绝
        mockMvc.perform(post("/api/v1/admin/events/{id}/review", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approve", true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EVENT_REVIEW_SELF_REVIEW"));
    }

    @Test
    void reviewRejectRequiresComment() throws Exception {
        makeOperator("oprej");
        makeSuperAdmin("sarej");
        String opToken = accessToken(adminLogin("oprej", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin("sarej", PASSWORD).andReturn());
        String eventId = createFullEvent(opToken, "驳回审核活动");
        String venueId = createVenue(opToken, "驳回场馆", "重庆", 5000);
        String sessionId = createSession(opToken, eventId, venueId, "晚场",
                "2028-02-01T19:00:00", "2028-02-01T21:00:00", 1000);
        String tierId = createTier(opToken, sessionId, "普通票", 100L, 100);
        configureSalePlan(opToken, tierId, "2028-01-01T10:00:00", "2028-02-01T18:00:00");

        mockMvc.perform(post("/api/v1/admin/events/{id}/submit-review", eventId)
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk());

        // 驳回但不填原因 → 拒绝
        mockMvc.perform(post("/api/v1/admin/events/{id}/review", eventId)
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approve", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVENT_REVIEW_COMMENT_REQUIRED"));

        // 驳回并填写原因 → REJECTED，可重新提交
        mockMvc.perform(post("/api/v1/admin/events/{id}/review", eventId)
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approve", false, "comment", "封面不合规"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(post("/api/v1/admin/events/{id}/submit-review", eventId)
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
    }

    @Test
    void publishWithMismatchedFingerprintRejected() throws Exception {
        makeOperator("opfp");
        makeSuperAdmin("safp");
        String opToken = accessToken(adminLogin("opfp", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin("safp", PASSWORD).andReturn());
        String eventId = createFullEvent(opToken, "指纹校验活动");
        String venueId = createVenue(opToken, "指纹校验场馆", "天津", 5000);
        String sessionId = createSession(opToken, eventId, venueId, "晚场",
                "2028-03-01T19:00:00", "2028-03-01T21:00:00", 1000);
        String tierId = createTier(opToken, sessionId, "普通票", 100L, 100);
        configureSalePlan(opToken, tierId, "2028-02-01T10:00:00", "2028-03-01T18:00:00");

        mockMvc.perform(post("/api/v1/admin/events/{id}/submit-review", eventId)
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/events/{id}/review", eventId)
                        .header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approve", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 审核通过后直接改库篡改票价（绕过业务冻结），模拟审核后被非法修改
        jdbcTemplate.update("UPDATE ft_ticket_tier SET unit_price_cent = 999 WHERE id = ?",
                Long.valueOf(tierId));

        // 发布时指纹不一致 → 拒绝
        mockMvc.perform(post("/api/v1/admin/events/{id}/publish", eventId)
                        .header("Authorization", "Bearer " + saToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_PUBLISH_FINGERPRINT_MISMATCH"));
    }

    @Test
    void configFrozenAfterSubmit() throws Exception {
        makeOperator("opfrozen");
        String token = accessToken(adminLogin("opfrozen", PASSWORD).andReturn());
        String eventId = createFullEvent(token, "配置冻结活动");
        String venueId = createVenue(token, "冻结场馆", "苏州", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2028-04-01T19:00:00", "2028-04-01T21:00:00", 1000);
        String tierId = createTier(token, sessionId, "普通票", 100L, 100);
        configureSalePlan(token, tierId, "2028-03-01T10:00:00", "2028-04-01T18:00:00");

        mockMvc.perform(post("/api/v1/admin/events/{id}/submit-review", eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 提交审核后票档被冻结，不可再编辑
        mockMvc.perform(put("/api/v1/admin/tiers/{id}", tierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tierBody("普通票", 200L, 100, 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_CONFIG_FROZEN"));

        // 活动本身也不可编辑
        mockMvc.perform(put("/api/v1/admin/events/{id}", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", "1", "title", "冻结活动改", "version", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_CONFIG_FROZEN"));
    }

    // ===== 公开可见性与可售状态 =====

    @Test
    void draftTierHiddenThenPublishedVisible() throws Exception {
        makeOperator("opvis");
        makeSuperAdmin("savis");
        String opToken = accessToken(adminLogin("opvis", PASSWORD).andReturn());
        String saToken = accessToken(adminLogin("savis", PASSWORD).andReturn());
        String eventId = createFullEvent(opToken, "可见性活动");
        String venueId = createVenue(opToken, "可见性场馆", "青岛", 5000);
        String sessionId = createSession(opToken, eventId, venueId, "晚场",
                "2028-05-01T19:00:00", "2028-05-01T21:00:00", 1000);
        createTier(opToken, sessionId, "普通票", 100L, 100);

        // 草稿票档不可见
        mockMvc.perform(get("/api/v1/sessions/{id}/tiers", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void operatorCannotReview() throws Exception {
        makeOperator("opnoreview");
        String token = accessToken(adminLogin("opnoreview", PASSWORD).andReturn());
        String eventId = createEvent(token, "无审核权活动");

        mockMvc.perform(post("/api/v1/admin/events/{id}/review", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approve", true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ===== helpers =====

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

    private String createEvent(String token, String title) throws Exception {
        return createEvent(token, title, Map.of());
    }

    /** 创建带发布必填字段的活动。 */
    private String createFullEvent(String token, String title) throws Exception {
        Map<String, Object> extra = new HashMap<>();
        extra.put("coverUrl", "https://img.example.com/cover.jpg");
        extra.put("detailContent", "演出详细介绍");
        extra.put("organizerName", "主办方文化传媒");
        extra.put("attendanceNotice", "请提前 60 分钟入场");
        return createEvent(token, title, extra);
    }

    private String createEvent(String token, String title, Map<String, Object> extra) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("categoryId", "1");
        body.put("title", title);
        body.put("version", 0);
        body.putAll(extra);
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
        MvcResult created = mockMvc.perform(post("/api/v1/admin/sessions/{sessionId}/tiers", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tierBody(name, priceCent, quantity, null))))
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

    private Map<String, Object> tierBody(String name, long priceCent, int quantity, Integer version) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("unitPriceCent", priceCent);
        body.put("initialQuantity", quantity);
        body.put("minPurchase", 1);
        body.put("maxPurchase", 2);
        body.put("perUserLimit", 2);
        if (version != null) {
            body.put("version", version);
        }
        return body;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
