package com.flashticket.identity.service;

import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.RegisteredUser;
import com.flashticket.identity.domain.RoleCode;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 超级管理员安全引导（见 doc/用户与账号模块设计.md 八）：
 * 不在代码或种子数据中硬编码生产密码，而是应用启动时若系统中不存在
 * SUPER_ADMIN，则按配置（环境变量/开发配置）创建初始超级管理员。
 *
 * <p>安全约束：默认关闭（enabled=false），生产由外部密钥注入并保持关闭；
 * 引导账号初始密码仅存在于配置，不打印到日志。
 */
@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);

    private final UserDomainService userDomainService;
    private final RbacDomainService rbacDomainService;
    private final PasswordEncoder passwordEncoder;

    private final boolean enabled;
    private final String username;
    private final String email;
    private final String password;

    public SuperAdminBootstrap(UserDomainService userDomainService,
                               RbacDomainService rbacDomainService,
                               PasswordEncoder passwordEncoder,
                               @Value("${flashticket.bootstrap.super-admin.enabled:false}") boolean enabled,
                               @Value("${flashticket.bootstrap.super-admin.username:superadmin}") String username,
                               @Value("${flashticket.bootstrap.super-admin.email:superadmin@flashticket.local}") String email,
                               @Value("${flashticket.bootstrap.super-admin.password:}") String password) {
        this.userDomainService = userDomainService;
        this.rbacDomainService = rbacDomainService;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (rbacDomainService.countActiveSuperAdmins() > 0) {
            log.info("已存在有效的超级管理员，跳过引导创建");
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("引导创建超级管理员已启用，但未配置初始密码（flashticket.bootstrap.super-admin.password），跳过");
            return;
        }
        if (password.length() < 12 || password.length() > 64) {
            log.warn("引导超级管理员初始密码长度须为 12-64 位，跳过创建");
            return;
        }
        PasswordPolicy.assertStrong(password);

        RegisteredUser created = userDomainService.createUser(
                username, email.toLowerCase(), username, passwordEncoder.encode(password));
        Role superAdminRole = rbacDomainService.findRoleByCode(RoleCode.SUPER_ADMIN);
        if (superAdminRole == null) {
            throw new IllegalStateException("系统缺少 SUPER_ADMIN 角色种子数据");
        }
        rbacDomainService.assignUserRoles(created.userId(), List.of(superAdminRole.getId()));
        log.info("已引导创建初始超级管理员：{}", username);
    }
}
