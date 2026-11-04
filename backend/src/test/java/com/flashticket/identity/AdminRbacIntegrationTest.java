package com.flashticket.identity;

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
import org.springframework.beans.factory.annotation.Value;
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
 * M03 后台账号与 RBAC 集成测试：真实 MySQL 8.4 + Redis 7.4。
 *
 * <p>覆盖：后台登录边界、方法级权限点授权、越权授予限制、自我提权限制、
 * 最后超级管理员保护、禁用/角色变更/重置密码后的会话失效、员工 CRUD、审计。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AdminRbacIntegrationTest {

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

    @Value("${flashticket.bootstrap.super-admin.username}")
    private String superAdminUsername;

    @Value("${flashticket.bootstrap.super-admin.password}")
    private String superAdminPassword;

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

    // ===== 后台登录边界 =====

    @Test
    void normalUserCannotLoginAdminPortal() throws Exception {
        registerUser("plainuser", "plainuser@test.com");

        adminLogin("plainuser", PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ADMIN_FORBIDDEN"));
    }

    @Test
    void normalUserCannotAccessAdminApi() throws Exception {
        registerUser("plainapi", "plainapi@test.com");
        String token = accessToken(userLogin("plainapi", PASSWORD).andReturn());

        // 已认证但无 admin 权限点 → 403 FORBIDDEN（方法级授权）
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanLoginAndListUsers() throws Exception {
        makeAdmin("adminlist");
        String token = accessToken(adminLogin("adminlist", PASSWORD).andReturn());

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    // ===== 越权限制 =====

    @Test
    void adminCannotGrantSuperAdminRole() throws Exception {
        makeAdmin("admingrant");
        String token = accessToken(adminLogin("admingrant", PASSWORD).andReturn());
        Long superAdminRoleId = rbacDomainService.findRoleByCode("SUPER_ADMIN").getId();

        mockMvc.perform(post("/api/v1/admin/staff")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "newsuper", "email", "newsuper@test.com",
                                "nickname", "新超管", "password", "NewSuper123456",
                                "roleIds", List.of(superAdminRoleId)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RBAC_ROLE_NOT_GRANTABLE"));
    }

    @Test
    void adminCannotEscalateSelf() throws Exception {
        makeAdmin("adminesc");
        String token = accessToken(adminLogin("adminesc", PASSWORD).andReturn());
        Long selfId = userDomainService.findUserByAccount("adminesc").getId();
        Long superAdminRoleId = rbacDomainService.findRoleByCode("SUPER_ADMIN").getId();
        Long adminRoleId = rbacDomainService.findRoleByCode("ADMIN").getId();

        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", selfId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(adminRoleId, superAdminRoleId)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RBAC_ROLE_NOT_GRANTABLE"));
    }

    @Test
    void adminCannotConfigureRolePermissions() throws Exception {
        makeAdmin("adminnoconf");
        String token = accessToken(adminLogin("adminnoconf", PASSWORD).andReturn());
        Long adminRoleId = rbacDomainService.findRoleByCode("ADMIN").getId();

        mockMvc.perform(put("/api/v1/admin/roles/{id}/permissions", adminRoleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("permissionIds", List.of(108)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void superAdminCanConfigureRolePermissions() throws Exception {
        String token = accessToken(adminLogin(superAdminUsername, superAdminPassword).andReturn());
        Long operatorRoleId = rbacDomainService.findRoleByCode("OPERATOR").getId();
        Long userListPermId = 101L;

        // 给 OPERATOR 授予 admin:user:list
        mockMvc.perform(put("/api/v1/admin/roles/{id}/permissions", operatorRoleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("permissionIds", List.of(userListPermId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 校验角色权限已更新
        mockMvc.perform(get("/api/v1/admin/roles/{id}/permissions", operatorRoleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("admin:user:list"));
    }

    // ===== 会话即时失效 =====

    @Test
    void disableAccountRevokesSessionImmediately() throws Exception {
        registerUser("victim1", "victim1@test.com");
        String victimToken = accessToken(userLogin("victim1", PASSWORD).andReturn());
        Long victimId = userDomainService.findUserByAccount("victim1").getId();

        makeAdmin("admindis");
        String adminToken = accessToken(adminLogin("admindis", PASSWORD).andReturn());

        mockMvc.perform(put("/api/v1/admin/users/{id}/status", victimId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DISABLED"))))
                .andExpect(status().isOk());

        // 被禁用用户原 token 立即失效
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + victimToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangeRevokesTargetSession() throws Exception {
        makeAdmin("admintarget");
        String targetToken = accessToken(adminLogin("admintarget", PASSWORD).andReturn());
        Long targetId = userDomainService.findUserByAccount("admintarget").getId();
        Long userRoleId = rbacDomainService.findRoleByCode("USER").getId();

        String superToken = accessToken(adminLogin(superAdminUsername, superAdminPassword).andReturn());

        // 超级管理员把 admintarget 降为纯 USER，触发会话撤销
        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", targetId)
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("roleIds", List.of(userRoleId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPasswordRevokesSession() throws Exception {
        makeAdmin("adminrpw");
        String oldToken = accessToken(adminLogin("adminrpw", PASSWORD).andReturn());
        Long targetId = userDomainService.findUserByAccount("adminrpw").getId();

        String superToken = accessToken(adminLogin(superAdminUsername, superAdminPassword).andReturn());

        mockMvc.perform(post("/api/v1/admin/users/{id}/reset-password", targetId)
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newPassword", "ResetNewPass2026"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
    }

    // ===== 最后超级管理员保护 =====

    @Test
    void lastSuperAdminCannotBeDisabled() throws Exception {
        makeAdmin("adminlast");
        String adminToken = accessToken(adminLogin("adminlast", PASSWORD).andReturn());
        Long superAdminId = userDomainService.findUserByAccount(superAdminUsername).getId();

        mockMvc.perform(put("/api/v1/admin/users/{id}/status", superAdminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DISABLED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RBAC_LAST_SUPER_ADMIN"));
    }

    // ===== 员工管理 =====

    @Test
    void createStaffThenListFilters() throws Exception {
        makeAdmin("adminstaff");
        String token = accessToken(adminLogin("adminstaff", PASSWORD).andReturn());
        Long operatorRoleId = rbacDomainService.findRoleByCode("OPERATOR").getId();

        // 创建员工
        mockMvc.perform(post("/api/v1/admin/staff")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "opstaff01", "email", "opstaff01@test.com",
                                "nickname", "运营小王", "password", "OpStaffPass2026",
                                "roleIds", List.of(operatorRoleId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("opstaff01"));

        // 按用户名模糊筛选
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .param("username", "opstaff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("opstaff01"));

        // 按角色筛选
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .param("role", "OPERATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber());

        // 审计日志应记录创建操作
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].action").value("USER_CREATE"));
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

    /** 注册普通用户并直接赋予指定角色（准备后台测试账号，密码保持 PASSWORD）。 */
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
}
