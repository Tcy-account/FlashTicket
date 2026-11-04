package com.flashticket.identity.domain;

import com.flashticket.identity.entity.User;
import com.flashticket.identity.entity.UserProfile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户领域服务接口（模块边界：编排服务 → 领域服务接口 → 数据访问层，
 * 见 doc/跨域一致性与工程落地审查.md 6.1）。
 *
 * <p>本接口只暴露用户域的领域能力，不向其他模块泄露 Mapper 细节。
 */
public interface UserDomainService {

    /** 校验用户名可用，占用则抛 AUTH_USERNAME_TAKEN。 */
    void assertUsernameAvailable(String username);

    /** 校验邮箱可用（入参应为规范化后的邮箱），占用则抛 AUTH_EMAIL_TAKEN。 */
    void assertEmailAvailable(String normalizedEmail);

    /**
     * 创建注册用户：ft_user + ft_user_profile + 默认 USER 角色关系。
     */
    RegisteredUser createRegisteredUser(String username, String normalizedEmail, String passwordHash);

    /**
     * 创建用户（ft_user + ft_user_profile，不含角色关系），用于后台创建员工等
     * 需要自定义角色的场景。返回领域产出。
     */
    RegisteredUser createUser(String username, String normalizedEmail, String nickname, String passwordHash);

    /** 按账号（用户名或规范化邮箱）查找用户，不存在返回 null（用于登录，不泄露存在性）。 */
    User findUserByAccount(String account);

    /** 按 ID 查找用户，不存在返回 null。 */
    User findUserById(Long userId);

    /** 查询用户角色码列表（如 ["USER"]）。 */
    List<String> findRoleCodes(Long userId);

    /** 查询用户资料，不存在返回 null。 */
    UserProfile findProfile(Long userId);

    /** 更新最后登录时间（UTC）。 */
    void updateLastLoginAt(Long userId, LocalDateTime time);

    /** 更新用户密码哈希。 */
    void updatePasswordHash(Long userId, String passwordHash);

    /** 更新用户资料（昵称/头像）。 */
    void updateProfile(Long userId, String nickname, String avatarUrl);

    /** 更新账号主状态（ACTIVE/DISABLED 等）。 */
    void updateStatus(Long userId, String status);

    /** 更新用户邮箱（入参应为规范化后的邮箱）。 */
    void updateEmail(Long userId, String normalizedEmail);

    /** 批量查询用户昵称，返回 userId → nickname 映射（无资料则缺省）。 */
    Map<Long, String> findNicknames(List<Long> userIds);
}
