package com.flashticket.identity.domain;

import com.flashticket.identity.entity.Permission;
import com.flashticket.identity.entity.Role;

import java.util.List;

/**
 * RBAC 领域服务接口（模块边界：编排服务 → 领域服务接口 → 数据访问层，
 * 见 doc/跨域一致性与工程落地审查.md 6.1）。
 *
 * <p>职责：角色/权限查询、用户权限码聚合、角色权限配置、用户角色分配，
 * 以及「最后一个超级管理员」保护所需的统计能力。
 * 越权校验等含操作者身份的业务规则由上层编排服务负责，本接口保持数据纯操作。
 */
public interface RbacDomainService {

    /** 聚合用户所有角色的有效权限码（去重），用于 JWT 与前端渲染。 */
    List<String> findPermissionCodes(Long userId);

    /** 全量角色列表。 */
    List<Role> listRoles();

    /** 按角色码查角色，不存在返回 null。 */
    Role findRoleByCode(String code);

    /** 按 ID 查角色，不存在返回 null。 */
    Role findRoleById(Long roleId);

    /** 按 ID 集合查角色（保持入参顺序）。 */
    List<Role> findRolesByIds(List<Long> roleIds);

    /** 全量有效权限点列表。 */
    List<Permission> listPermissions();

    /** 某角色已绑定的权限点。 */
    List<Permission> findPermissionsByRoleId(Long roleId);

    /** 配置角色权限点（整体替换，事务原子）。 */
    void grantRolePermissions(Long roleId, List<Long> permissionIds);

    /** 分配用户角色（整体替换，事务原子）。 */
    void assignUserRoles(Long userId, List<Long> roleIds);

    /** 统计账号仍 ACTIVE 的超级管理员数量。 */
    long countActiveSuperAdmins();

    /** 用户是否持有 SUPER_ADMIN 角色且账号 ACTIVE。 */
    boolean isActiveSuperAdmin(Long userId);

    /** 查询持有某角色的全部用户 ID（用于权限变更后撤销会话）。 */
    List<Long> findUserIdsByRoleId(Long roleId);
}
