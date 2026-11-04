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
 * M05 演出活动与场次管理集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：草稿活动游客不可见、已发布活动可见、普通用户/ADMIN 无演出域权限、
 * OPERATOR 活动 CRUD、乐观锁版本冲突、场次创建、同场馆时间冲突（含清场缓冲）、
 * 容量校验、延期/取消状态机、非法状态转换。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class EventSessionIntegrationTest {

    private static final String PASSWORD = "SecurePass1234";
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

    // ===== 公开可见性 =====

    @Test
    void draftEventHiddenFromPublic() throws Exception {
        makeOperator("opdraft");
        String token = accessToken(adminLogin("opdraft", PASSWORD).andReturn());
        String eventId = createEvent(token, "草稿演唱会");
        String venueId = createVenue(token, "草稿场馆", "北京", 10000);

        // 草稿活动不应出现在公开列表
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        // 草稿详情对游客不可见
        mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));

        // 草稿场次亦不可见
        createSession(token, eventId, venueId, "晚场", "2027-05-01T19:00:00", "2027-05-01T21:00:00", 1000);
        mockMvc.perform(get("/api/v1/events/{id}/sessions", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void publishedEventVisibleToPublic() throws Exception {
        makeOperator("oppub");
        String token = accessToken(adminLogin("oppub", PASSWORD).andReturn());
        String eventId = createEvent(token, "已发布音乐节");
        String venueId = createVenue(token, "发布场馆", "上海", 8000);
        String sessionId = createSession(token, eventId, venueId, "主舞台",
                "2027-06-01T19:00:00", "2027-06-01T22:00:00", 3000);

        // M05 无发布接口，直接置 PUBLISHED 模拟审核发布结果
        jdbcTemplate.update("UPDATE ft_event SET status = 'PUBLISHED', published_at = NOW(3) WHERE id = ?",
                Long.valueOf(eventId));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("已发布音乐节"));

        mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/events/{id}/sessions", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(sessionId));
    }

    // ===== 权限边界 =====

    @Test
    void normalUserCannotAccessAdminEventApi() throws Exception {
        registerUser("evtuser", "evtuser@test.com");
        String token = accessToken(userLogin("evtuser", PASSWORD).andReturn());

        mockMvc.perform(get("/api/v1/admin/events").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminWithoutEventPermissionCannotManageEvent() throws Exception {
        makeAdmin("admevent");
        String token = accessToken(adminLogin("admevent", PASSWORD).andReturn());

        mockMvc.perform(get("/api/v1/admin/events").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ===== 活动 CRUD 与版本控制 =====

    @Test
    void operatorCanCreateAndUpdateEvent() throws Exception {
        makeOperator("opevt");
        String token = accessToken(adminLogin("opevt", PASSWORD).andReturn());

        MvcResult created = mockMvc.perform(post("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", "1", "title", "测试演唱会", "subtitle", "副标题",
                                "organizerName", "主办方", "version", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn();
        String id = json(created).get("id").asText();

        // 编辑（回传 version=0）
        mockMvc.perform(put("/api/v1/admin/events/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", "2", "title", "测试演唱会改", "version", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("测试演唱会改"))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void eventUpdateWithStaleVersionRejected() throws Exception {
        makeOperator("opver");
        String token = accessToken(adminLogin("opver", PASSWORD).andReturn());
        String id = createEvent(token, "版本冲突活动");

        // 用过期 version=0（当前已是 1，因为上面 createEvent 内部无编辑，version 仍为 0）
        // 先编辑一次使 version 变为 1
        mockMvc.perform(put("/api/v1/admin/events/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", "1", "title", "版本冲突活动v1", "version", 0))))
                .andExpect(status().isOk());

        // 再拿过期 version=0 编辑 → 版本冲突
        mockMvc.perform(put("/api/v1/admin/events/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", "1", "title", "版本冲突活动v2", "version", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_VERSION_CONFLICT"));
    }

    @Test
    void operatorCannotCancelEvent() throws Exception {
        makeOperator("opcancel");
        String token = accessToken(adminLogin("opcancel", PASSWORD).andReturn());
        String id = createEvent(token, "不可取消活动");

        mockMvc.perform(post("/api/v1/admin/events/{id}/cancel", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "取消"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void superAdminCanCancelEvent() throws Exception {
        makeSuperAdmin("saevt");
        String token = accessToken(adminLogin("saevt", PASSWORD).andReturn());
        String id = createEvent(token, "超管取消活动");

        mockMvc.perform(post("/api/v1/admin/events/{id}/cancel", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "档期冲突"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // 已取消不可再取消
        mockMvc.perform(post("/api/v1/admin/events/{id}/cancel", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "再次取消"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_CANCELLABLE"));
    }

    // ===== 场次：创建与时间冲突 =====

    @Test
    void operatorCanCreateSession() throws Exception {
        makeOperator("opse");
        String token = accessToken(adminLogin("opse", PASSWORD).andReturn());
        String eventId = createEvent(token, "场次测试活动");
        String venueId = createVenue(token, "场次场馆", "广州", 5000);

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/sessions", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody(
                                venueId, "晚场", "2027-07-01T19:00:00", "2027-07-01T21:00:00", 2000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.saleCapacity").value(2000));
    }

    @Test
    void overlappingSessionRejected() throws Exception {
        makeOperator("opconf");
        String token = accessToken(adminLogin("opconf", PASSWORD).andReturn());
        String eventId = createEvent(token, "冲突测试活动");
        String venueId = createVenue(token, "冲突场馆", "深圳", 10000);

        // 场次 A：19:00-21:00
        createSession(token, eventId, venueId, "A", "2027-08-01T19:00:00", "2027-08-01T21:00:00", 1000);

        // 场次 B：20:30-22:00 与 A 重叠 → 冲突
        mockMvc.perform(post("/api/v1/admin/events/{eventId}/sessions", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody(
                                venueId, "B", "2027-08-01T20:30:00", "2027-08-01T22:00:00", 1000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_SESSION_TIME_CONFLICT"));
    }

    @Test
    void sessionWithinClearBufferRejected() throws Exception {
        makeOperator("opbuffer");
        String token = accessToken(adminLogin("opbuffer", PASSWORD).andReturn());
        String eventId = createEvent(token, "缓冲测试活动");
        String venueId = createVenue(token, "缓冲场馆", "成都", 10000);

        // 场次 A：19:00-21:00
        createSession(token, eventId, venueId, "A", "2027-09-01T19:00:00", "2027-09-01T21:00:00", 1000);

        // 场次 B：21:10 开始，距 A 结束仅 10 分钟（< 30 分钟清场缓冲）→ 冲突
        mockMvc.perform(post("/api/v1/admin/events/{eventId}/sessions", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody(
                                venueId, "B", "2027-09-01T21:10:00", "2027-09-01T23:00:00", 1000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_SESSION_TIME_CONFLICT"));

        // 场次 C：21:30 开始，正好 30 分钟缓冲 → 不冲突
        mockMvc.perform(post("/api/v1/admin/events/{eventId}/sessions", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody(
                                venueId, "C", "2027-09-01T21:30:00", "2027-09-01T23:30:00", 1000))))
                .andExpect(status().isOk());
    }

    @Test
    void sessionCapacityExceededRejected() throws Exception {
        makeOperator("opcap");
        String token = accessToken(adminLogin("opcap", PASSWORD).andReturn());
        String eventId = createEvent(token, "容量测试活动");
        String venueId = createVenue(token, "容量场馆", "杭州", 5000);

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/sessions", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody(
                                venueId, "超容场次", "2027-10-01T19:00:00", "2027-10-01T21:00:00", 6000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVENT_SESSION_CAPACITY_EXCEED"));
    }

    @Test
    void sessionInvalidTimeRejected() throws Exception {
        makeOperator("opbadtime");
        String token = accessToken(adminLogin("opbadtime", PASSWORD).andReturn());
        String eventId = createEvent(token, "时间测试活动");
        String venueId = createVenue(token, "时间场馆", "南京", 5000);

        // end_at 早于 start_at
        mockMvc.perform(post("/api/v1/admin/events/{eventId}/sessions", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody(
                                venueId, "非法时间", "2027-11-01T21:00:00", "2027-11-01T19:00:00", 1000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVENT_SESSION_INVALID_TIME"));
    }

    // ===== 场次：状态机 =====

    @Test
    void sessionPostponeAndCancelFlow() throws Exception {
        makeSuperAdmin("sases");
        String token = accessToken(adminLogin("sases", PASSWORD).andReturn());
        String eventId = createEvent(token, "状态机活动");
        String venueId = createVenue(token, "状态机场馆", "武汉", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2027-12-01T19:00:00", "2027-12-01T21:00:00", 1000);

        // 延期：SCHEDULED → POSTPONED
        mockMvc.perform(post("/api/v1/admin/sessions/{id}/postpone", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "艺人行程变更"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("POSTPONED"));

        // 延期后不可再延期
        mockMvc.perform(post("/api/v1/admin/sessions/{id}/postpone", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "再次延期"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_SESSION_ILLEGAL_TRANSITION"));

        // 取消：POSTPONED → CANCELLED
        mockMvc.perform(post("/api/v1/admin/sessions/{id}/cancel", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "演出取消"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void operatorCannotCancelSession() throws Exception {
        makeOperator("opseccancel");
        String token = accessToken(adminLogin("opseccancel", PASSWORD).andReturn());
        String eventId = createEvent(token, "运营不可取消场次");
        String venueId = createVenue(token, "运营场次场馆", "西安", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2028-01-01T19:00:00", "2028-01-01T21:00:00", 1000);

        // OPERATOR 无 event:session:cancel 权限
        mockMvc.perform(post("/api/v1/admin/sessions/{id}/cancel", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "取消"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void sessionUpdateWithStaleVersionRejected() throws Exception {
        makeOperator("opsever");
        String token = accessToken(adminLogin("opsever", PASSWORD).andReturn());
        String eventId = createEvent(token, "场次版本活动");
        String venueId = createVenue(token, "场次版本场馆", "重庆", 5000);
        String sessionId = createSession(token, eventId, venueId, "晚场",
                "2028-02-01T19:00:00", "2028-02-01T21:00:00", 1000);

        // 用过期 version=0 编辑（创建后 version=0，先编辑一次变 1）
        mockMvc.perform(put("/api/v1/admin/sessions/{id}", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody(
                                venueId, "晚场v1", "2028-02-01T19:00:00", "2028-02-01T21:00:00", 1000, 0))))
                .andExpect(status().isOk());

        // 再拿 version=0 → 冲突
        mockMvc.perform(put("/api/v1/admin/sessions/{id}", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody(
                                venueId, "晚场v2", "2028-02-01T19:00:00", "2028-02-01T21:00:00", 1000, 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_SESSION_VERSION_CONFLICT"));
    }

    // ===== helpers =====

    private ResultActions registerUser(String username, String email) throws Exception {
        String sendBody = objectMapper.writeValueAsString(Map.of("email", email, "purpose", "REGISTER"));
        mockMvc.perform(post("/api/v1/auth/verify-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendBody))
                .andExpect(status().isOk());

        String code = redisTemplate.opsForValue().get(REGISTER_CODE_KEY + email.toLowerCase(Locale.ROOT));
        assertThat(code).isNotNull();

        String regBody = objectMapper.writeValueAsString(Map.of(
                "username", username, "email", email, "password", PASSWORD, "verifyCode", code));
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regBody));
    }

    private void makeOperator(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        Long userRoleId = rbacDomainService.findRoleByCode("USER").getId();
        Long operatorRoleId = rbacDomainService.findRoleByCode("OPERATOR").getId();
        rbacDomainService.assignUserRoles(user.getId(), List.of(userRoleId, operatorRoleId));
    }

    private void makeAdmin(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        Long userRoleId = rbacDomainService.findRoleByCode("USER").getId();
        Long adminRoleId = rbacDomainService.findRoleByCode("ADMIN").getId();
        rbacDomainService.assignUserRoles(user.getId(), List.of(userRoleId, adminRoleId));
    }

    private void makeSuperAdmin(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        Long userRoleId = rbacDomainService.findRoleByCode("USER").getId();
        Long superAdminRoleId = rbacDomainService.findRoleByCode("SUPER_ADMIN").getId();
        rbacDomainService.assignUserRoles(user.getId(), List.of(userRoleId, superAdminRoleId));
    }

    private ResultActions userLogin(String account, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("account", account, "password", password))));
    }

    private ResultActions adminLogin(String account, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("account", account, "password", password))));
    }

    private String accessToken(MvcResult loginResult) throws Exception {
        JsonNode data = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
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
        MvcResult created = mockMvc.perform(post("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", "1", "title", title, "version", 0))))
                .andExpect(status().isOk())
                .andReturn();
        return json(created).get("id").asText();
    }

    private String createSession(String token, String eventId, String venueId, String name,
                                 String startAt, String endAt, int saleCapacity) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/admin/events/{eventId}/sessions", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                sessionBody(venueId, name, startAt, endAt, saleCapacity))))
                .andExpect(status().isOk())
                .andReturn();
        return json(created).get("id").asText();
    }

    private Map<String, Object> sessionBody(String venueId, String name, String startAt,
                                            String endAt, int saleCapacity) {
        return sessionBody(venueId, name, startAt, endAt, saleCapacity, null);
    }

    private Map<String, Object> sessionBody(String venueId, String name, String startAt,
                                            String endAt, int saleCapacity, Integer version) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("venueId", venueId);
        body.put("name", name);
        body.put("startAt", startAt);
        body.put("endAt", endAt);
        body.put("saleCapacity", saleCapacity);
        if (version != null) {
            body.put("version", version);
        }
        return body;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
