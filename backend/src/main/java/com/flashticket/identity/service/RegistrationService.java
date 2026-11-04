package com.flashticket.identity.service;

import com.flashticket.identity.domain.RegisteredUser;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.dto.RegisterRequest;
import com.flashticket.identity.dto.RegisterResponse;
import com.flashticket.identity.enums.VerifyCodePurpose;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 注册编排服务（应用层）：串联校验、验证码核销、账号创建，控制本地事务边界。
 *
 * <p>流程：规范化 → 密码强度 → 唯一性预检查 → 验证码原子核销 → 创建账号。
 * 唯一性预检查先于验证码核销，避免唯一冲突时白白消耗验证码；并发冲突由数据库
 * 唯一索引兜底（见 UserDomainServiceImpl）。
 */
@Service
public class RegistrationService {

    private final UserDomainService userDomainService;
    private final VerificationCodeService verificationCodeService;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserDomainService userDomainService,
                               VerificationCodeService verificationCodeService,
                               PasswordEncoder passwordEncoder) {
        this.userDomainService = userDomainService;
        this.verificationCodeService = verificationCodeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String username = req.getUsername().trim();
        String email = normalizeEmail(req.getEmail());

        PasswordPolicy.assertStrong(req.getPassword());

        // 唯一性预检查：先于验证码核销，快速失败
        userDomainService.assertUsernameAvailable(username);
        userDomainService.assertEmailAvailable(email);

        // 验证码原子核销（Redis，成功后一次性作废）
        verificationCodeService.verify(email, VerifyCodePurpose.REGISTER, req.getVerifyCode());

        // 创建账号（同一 MySQL 本地事务：user + profile + user_role）
        String passwordHash = passwordEncoder.encode(req.getPassword());
        RegisteredUser user = userDomainService.createRegisteredUser(username, email, passwordHash);

        return new RegisterResponse(user.userId(), user.username(), user.email(), user.nickname());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
