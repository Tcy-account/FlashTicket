package com.flashticket.identity.domain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.identity.domain.RbacDomainService;
import com.flashticket.identity.domain.RoleCode;
import com.flashticket.identity.entity.Permission;
import com.flashticket.identity.entity.Role;
import com.flashticket.identity.entity.RolePermission;
import com.flashticket.identity.entity.UserRole;
import com.flashticket.identity.mapper.PermissionMapper;
import com.flashticket.identity.mapper.RoleMapper;
import com.flashticket.identity.mapper.RolePermissionMapper;
import com.flashticket.identity.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RBAC 领域服务实现：角色/权限查询、权限码聚合、角色权限配置、用户角色分配。
 *
 * <p>「删除旧关系 + 插入新关系」在同一本地事务内原子完成，
 * 避免整体替换过程中出现中间态。
 */
@Service
public class RbacDomainServiceImpl implements RbacDomainService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    public RbacDomainServiceImpl(RoleMapper roleMapper,
                                 PermissionMapper permissionMapper,
                                 RolePermissionMapper rolePermissionMapper,
                                 UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public List<String> findPermissionCodes(Long userId) {
        return permissionMapper.findCodesByUserId(userId);
    }

    @Override
    public List<Role> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    @Override
    public Role findRoleByCode(String code) {
        return roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
    }

    @Override
    public Role findRoleById(Long roleId) {
        return roleMapper.selectById(roleId);
    }

    @Override
    public List<Role> findRolesByIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        Map<Long, Role> byId = roles.stream().collect(Collectors.toMap(Role::getId, Function.identity()));
        List<Role> ordered = new ArrayList<>(roleIds.size());
        for (Long id : roleIds) {
            Role role = byId.get(id);
            if (role != null) {
                ordered.add(role);
            }
        }
        return ordered;
    }

    @Override
    public List<Permission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getId));
    }

    @Override
    public List<Permission> findPermissionsByRoleId(Long roleId) {
        return permissionMapper.findByRoleId(roleId);
    }

    @Override
    @Transactional
    public void grantRolePermissions(Long roleId, List<Long> permissionIds) {
        Role role = findRoleById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds != null) {
            permissionIds.stream().distinct().forEach(permissionId -> {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            });
        }
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null) {
            roleIds.stream().distinct().forEach(roleId -> {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            });
        }
    }

    @Override
    public long countActiveSuperAdmins() {
        Long roleId = roleMapper.findIdByCode(RoleCode.SUPER_ADMIN);
        if (roleId == null) {
            return 0;
        }
        return userRoleMapper.countActiveUsersByRoleId(roleId);
    }

    @Override
    public boolean isActiveSuperAdmin(Long userId) {
        Long roleId = roleMapper.findIdByCode(RoleCode.SUPER_ADMIN);
        if (roleId == null) {
            return false;
        }
        return userRoleMapper.countByUserIdAndRoleId(userId, roleId) > 0;
    }

    @Override
    public List<Long> findUserIdsByRoleId(Long roleId) {
        return userRoleMapper.findUserIdsByRoleId(roleId);
    }
}
