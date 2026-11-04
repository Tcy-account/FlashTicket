package com.flashticket.identity.domain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.identity.domain.RegisteredUser;
import com.flashticket.identity.domain.UserDomainService;
import com.flashticket.identity.entity.Role;
import com.flashticket.identity.entity.User;
import com.flashticket.identity.entity.UserProfile;
import com.flashticket.identity.entity.UserRole;
import com.flashticket.identity.enums.UserStatus;
import com.flashticket.identity.mapper.RoleMapper;
import com.flashticket.identity.mapper.UserMapper;
import com.flashticket.identity.mapper.UserProfileMapper;
import com.flashticket.identity.mapper.UserRoleMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户领域服务实现：唯一性校验、账号创建、登录查找与资料维护。
 *
 * <p>账号创建不单独开启事务，由上层编排服务的本地事务统一提交，
 * 保证 ft_user / ft_user_profile / ft_user_role 三者同库原子（见跨域审查 6.1）。
 */
@Service
public class UserDomainServiceImpl implements UserDomainService {

    private static final String DEFAULT_ROLE_CODE = "USER";

    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public UserDomainServiceImpl(UserMapper userMapper,
                                 UserProfileMapper profileMapper,
                                 RoleMapper roleMapper,
                                 UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public void assertUsernameAvailable(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.AUTH_USERNAME_TAKEN);
        }
    }

    @Override
    public void assertEmailAvailable(String normalizedEmail) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, normalizedEmail));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.AUTH_EMAIL_TAKEN);
        }
    }

    @Override
    public RegisteredUser createRegisteredUser(String username, String normalizedEmail, String passwordHash) {
        RegisteredUser user = createUser(username, normalizedEmail, username, passwordHash);

        Role defaultRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, DEFAULT_ROLE_CODE));
        if (defaultRole == null) {
            throw new IllegalStateException("系统缺少默认角色 " + DEFAULT_ROLE_CODE + "，请检查种子数据");
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(user.userId());
        userRole.setRoleId(defaultRole.getId());
        userRoleMapper.insert(userRole);

        return user;
    }

    @Override
    public RegisteredUser createUser(String username, String normalizedEmail, String nickname, String passwordHash) {
        try {
            User user = new User();
            user.setUsername(username);
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordHash);
            user.setStatus(UserStatus.ACTIVE.name());
            userMapper.insert(user);

            UserProfile profile = new UserProfile();
            profile.setUserId(user.getId());
            profile.setNickname(nickname);
            profileMapper.insert(profile);

            return new RegisteredUser(user.getId(), user.getUsername(), user.getEmail(), profile.getNickname());
        } catch (DuplicateKeyException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("uk_email")) {
                throw new BizException(ErrorCode.AUTH_EMAIL_TAKEN);
            }
            throw new BizException(ErrorCode.AUTH_USERNAME_TAKEN);
        }
    }

    @Override
    public User findUserByAccount(String account) {
        account = account == null ? "" : account.trim();
        if (account.isEmpty()) {
            return null;
        }
        // 含 @ 视为邮箱（规范化小写）；否则按用户名（列 collation 为 ci，天然不区分大小写）
        if (account.contains("@")) {
            String email = account.toLowerCase(Locale.ROOT);
            return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, account));
    }

    @Override
    public User findUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public List<String> findRoleCodes(Long userId) {
        return roleMapper.findCodesByUserId(userId);
    }

    @Override
    public UserProfile findProfile(Long userId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
    }

    @Override
    public void updateLastLoginAt(Long userId, LocalDateTime time) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginAt(time);
        userMapper.updateById(user);
    }

    @Override
    public void updatePasswordHash(Long userId, String passwordHash) {
        User user = new User();
        user.setId(userId);
        user.setPasswordHash(passwordHash);
        userMapper.updateById(user);
    }

    @Override
    public void updateProfile(Long userId, String nickname, String avatarUrl) {
        UserProfile profile = findProfile(userId);
        if (profile == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户资料不存在");
        }
        UserProfile update = new UserProfile();
        update.setId(profile.getId());
        update.setNickname(nickname);
        update.setAvatarUrl(avatarUrl);
        profileMapper.updateById(update);
    }

    @Override
    public void updateStatus(Long userId, String status) {
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void updateEmail(Long userId, String normalizedEmail) {
        User user = new User();
        user.setId(userId);
        user.setEmail(normalizedEmail);
        userMapper.updateById(user);
    }

    @Override
    public Map<Long, String> findNicknames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserProfile> profiles = profileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds));
        return profiles.stream().collect(Collectors.toMap(
                UserProfile::getUserId, UserProfile::getNickname, (a, b) -> a));
    }
}
