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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
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
 * M04 演出分类与场馆管理集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：游客公开查询分类/场馆、普通用户/ADMIN 无权限、OPERATOR 分类与场馆
 * CRUD 及启停、分类 code 唯一性、场馆容量校验、城市筛选、停用分类前台隐藏。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class EventCatalogIntegrationTest {

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
    private UserDomainService userDomainService;

    @Autowired
    private RbacDomainService rbacDomainService;

    // ===== 公开查询 =====

    @Test
    void guestCanListCategories() throws Exception {
        // 断言种子分类按 sort_order 升序返回（CONCERT 权重最小，恒在首位）
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].code").value("CONCERT"));
    }

    @Test
    void guestCanListVenues() throws Exception {
        mockMvc.perform(get("/api/v1/venues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    // ===== 权限边界 =====

    @Test
    void normalUserCannotAccessAdminCategoryApi() throws Exception {
        registerUser("catuser", "catuser@test.com");
        String token = accessToken(userLogin("catuser", PASSWORD).andReturn());

        mockMvc.perform(get("/api/v1/admin/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminWithoutEventPermissionCannotManageVenue() throws Exception {
        makeAdmin("admvenue");
        String token = accessToken(adminLogin("admvenue", PASSWORD).andReturn());

        // ADMIN 拥有账号管理权限，但无 event:venue:list → 403
        mockMvc.perform(get("/api/v1/admin/venues").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ===== 分类管理 =====

    @Test
    void operatorCanCreateUpdateAndToggleCategory() throws Exception {
        makeOperator("opcat");
        String token = accessToken(adminLogin("opcat", PASSWORD).andReturn());

        // 创建
        MvcResult created = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "OPERA", "name", "歌剧", "description", "歌剧演出", "sortOrder", 70))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.code").value("OPERA"))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // 编辑
        mockMvc.perform(put("/api/v1/admin/categories/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "OPERA", "name", "歌剧艺术", "description", "歌剧演出", "sortOrder", 71))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("歌剧艺术"));

        // 停用
        mockMvc.perform(put("/api/v1/admin/categories/{id}/status", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DISABLED"))))
                .andExpect(status().isOk());

        // 后台列表仍可见（含停用），且包含刚创建的 OPERA 分类
        JsonNode list = getJson(mockMvc.perform(get("/api/v1/admin/categories")
                .header("Authorization", "Bearer " + token)));
        assertThat(codeList(list)).contains("OPERA");
    }

    @Test
    void categoryCodeMustBeUnique() throws Exception {
        makeOperator("opcatuniq");
        String token = accessToken(adminLogin("opcatuniq", PASSWORD).andReturn());

        // 种子已含 CONCERT，创建同码分类应冲突
        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "CONCERT", "name", "重复分类"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_CATEGORY_CODE_TAKEN"));
    }

    @Test
    void disabledCategoryHiddenFromPublic() throws Exception {
        makeOperator("opcathide");
        String token = accessToken(adminLogin("opcathide", PASSWORD).andReturn());

        // 创建专属分类后停用（避免影响种子分类，保证测试自包含）
        MvcResult created = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "HIDDEN_CAT", "name", "隐藏分类", "sortOrder", 999))))
                .andExpect(status().isOk())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // 停用
        mockMvc.perform(put("/api/v1/admin/categories/{id}/status", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DISABLED"))))
                .andExpect(status().isOk());

        // 公开列表不再包含该停用分类
        JsonNode list = getJson(mockMvc.perform(get("/api/v1/categories")));
        assertThat(codeList(list)).doesNotContain("HIDDEN_CAT");
    }

    // ===== 场馆管理 =====

    @Test
    void operatorCanCreateVenueAndFilterByCity() throws Exception {
        makeOperator("opvenue");
        String token = accessToken(adminLogin("opvenue", PASSWORD).andReturn());

        // 创建北京场馆
        mockMvc.perform(post("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "星光体育馆", "countryCode", "CN", "province", "北京市",
                                "city", "北京", "district", "朝阳区", "address", "朝阳区体育场路 1 号",
                                "timezone", "Asia/Shanghai", "maxCapacity", 10000,
                                "description", "综合体育馆"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.city").value("北京"));

        // 创建上海场馆
        mockMvc.perform(post("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "上海大剧院", "countryCode", "CN", "province", "上海市",
                                "city", "上海", "district", "黄浦区", "address", "黄浦区人民大道 300 号",
                                "timezone", "Asia/Shanghai", "maxCapacity", 1600,
                                "description", "剧院"))))
                .andExpect(status().isOk());

        // 城市筛选：北京 → 1 条
        mockMvc.perform(get("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token)
                        .param("city", "北京"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("星光体育馆"));

        // 公开列表：城市筛选北京
        mockMvc.perform(get("/api/v1/venues").param("city", "北京"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void venueCapacityMustBePositive() throws Exception {
        makeOperator("opvencap");
        String token = accessToken(adminLogin("opvencap", PASSWORD).andReturn());

        mockMvc.perform(post("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "非法场馆", "countryCode", "CN",
                                "city", "广州", "address", "某地址",
                                "timezone", "Asia/Shanghai", "maxCapacity", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"));
    }

    @Test
    void operatorCanToggleVenueAndHiddenFromPublic() throws Exception {
        makeOperator("opventoggle");
        String token = accessToken(adminLogin("opventoggle", PASSWORD).andReturn());

        MvcResult created = mockMvc.perform(post("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "成都体育馆", "countryCode", "CN", "province", "四川省",
                                "city", "成都", "district", "武侯区", "address", "武侯区某路",
                                "timezone", "Asia/Shanghai", "maxCapacity", 5000))))
                .andExpect(status().isOk())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // 停用
        mockMvc.perform(put("/api/v1/admin/venues/{id}/status", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DISABLED"))))
                .andExpect(status().isOk());

        // 公开列表不再返回该停用场馆
        mockMvc.perform(get("/api/v1/venues").param("city", "成都"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
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

    /** 注册普通用户并赋予 OPERATOR 角色（演出域后台账号，密码保持 PASSWORD）。 */
    private void makeOperator(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        Long userRoleId = rbacDomainService.findRoleByCode("USER").getId();
        Long operatorRoleId = rbacDomainService.findRoleByCode("OPERATOR").getId();
        rbacDomainService.assignUserRoles(user.getId(), List.of(userRoleId, operatorRoleId));
    }

    /** 注册普通用户并赋予 ADMIN 角色（账号域后台账号，不含演出域权限）。 */
    private void makeAdmin(String username) throws Exception {
        registerUser(username, username + "@test.com").andExpect(status().isOk());
        User user = userDomainService.findUserByAccount(username);
        Long userRoleId = rbacDomainService.findRoleByCode("USER").getId();
        Long adminRoleId = rbacDomainService.findRoleByCode("ADMIN").getId();
        rbacDomainService.assignUserRoles(user.getId(), List.of(userRoleId, adminRoleId));
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

    /** 执行并读取响应 JSON 根节点。 */
    private JsonNode getJson(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    /** 从列表响应中提取 code 列表（$.data 数组元素取 code 字段）。 */
    private List<String> codeList(JsonNode response) {
        List<String> codes = new ArrayList<>();
        JsonNode data = response.get("data");
        if (data != null && data.isArray()) {
            data.forEach(n -> {
                if (n.has("code")) {
                    codes.add(n.get("code").asText());
                }
            });
        }
        return codes;
    }
}
