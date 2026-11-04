-- =====================================================================
-- FlashTicket M03 · 后台账号与 RBAC
-- 建表：ft_permission / ft_role_permission / ft_operation_audit
-- 依据：doc/全局数据库模型与ER关系设计.md（四、用户与权限域）
--        doc/用户与账号模块设计.md（八、后台账号与 RBAC / 十、审计日志）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码
-- 说明：本里程碑只落地「身份域」权限点；后续 M04+ 演出/交易等权限点
--       由对应里程碑追加，遵循同一套 {域}:{资源}:{动作} 编码约定。
--       超级管理员账号不由种子硬编码，由 SuperAdminBootstrap 引导创建。
-- =====================================================================

-- 1) 权限点定义
CREATE TABLE ft_permission (
    id         BIGINT      NOT NULL COMMENT '雪花主键',
    code       VARCHAR(64) NOT NULL COMMENT '权限码，全局唯一：{域}:{资源}:{动作}',
    name       VARCHAR(64) NOT NULL COMMENT '权限显示名',
    type       VARCHAR(32) NOT NULL DEFAULT 'API' COMMENT '权限类型：MENU/ACTION/API',
    status     VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '权限状态：ACTIVE/DISABLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='权限点定义';

-- 2) 角色与权限点多对多关系
CREATE TABLE ft_role_permission (
    id            BIGINT      NOT NULL COMMENT '雪花主键',
    role_id       BIGINT      NOT NULL COMMENT '角色',
    permission_id BIGINT      NOT NULL COMMENT '权限点',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES ft_role (id),
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES ft_permission (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色与权限关系';

-- 3) 后台敏感操作审计
CREATE TABLE ft_operation_audit (
    id                BIGINT       NOT NULL COMMENT '雪花主键',
    operator_id       BIGINT       NOT NULL COMMENT '操作员用户 ID',
    operator_username VARCHAR(32)  NOT NULL COMMENT '操作员用户名（冗余快照，防用户变更后无法追溯）',
    action            VARCHAR(64)  NOT NULL COMMENT '操作类型：USER_CREATE/USER_DISABLE/ROLE_ASSIGN 等',
    target_type       VARCHAR(32)  NOT NULL COMMENT '目标类型：USER/ROLE',
    target_id         BIGINT       NULL COMMENT '目标 ID',
    detail            JSON         NULL COMMENT '变更详情快照（不含密码/Token/验证码等敏感内容）',
    ip                VARCHAR(64)  NULL COMMENT '操作来源 IP',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_operator_created (operator_id, created_at),
    KEY idx_target (target_type, target_id),
    KEY idx_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台敏感操作审计';

-- 4) 身份域权限点种子（固定 id 便于角色关系引用）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (101, 'admin:user:list',        '用户列表',     'API', 'ACTIVE'),
    (102, 'admin:user:create',      '创建员工',     'API', 'ACTIVE'),
    (103, 'admin:user:update',      '修改员工',     'API', 'ACTIVE'),
    (104, 'admin:user:status',      '启用/禁用账号', 'API', 'ACTIVE'),
    (105, 'admin:user:roles',       '分配角色',     'API', 'ACTIVE'),
    (106, 'admin:user:revoke',      '强制下线',     'API', 'ACTIVE'),
    (107, 'admin:user:reset-pwd',   '重置密码',     'API', 'ACTIVE'),
    (108, 'admin:role:list',        '角色列表',     'API', 'ACTIVE'),
    (109, 'admin:role:grant',       '配置角色权限', 'API', 'ACTIVE'),
    (110, 'admin:permission:list',  '权限点列表',   'API', 'ACTIVE'),
    (111, 'admin:audit:list',       '审计日志',     'API', 'ACTIVE');

-- 5) 角色-权限关系种子：明确五种角色的权限边界
--    USER        ：无任何后台权限
--    OPERATOR    ：无身份域权限（后台登录权，身份域权限留待 M04+ 演出域追加）
--    FINANCE     ：无身份域权限（后台登录权，财务权限留待 M11 退款审核追加）
--    ADMIN       ：完整账号管理 + 查看角色/权限/审计，但【不含】 admin:role:grant
--    SUPER_ADMIN ：全部身份域权限（含 admin:role:grant 配置角色权限）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- ADMIN (role_id = 4)
    (401, 4, 101), (402, 4, 102), (403, 4, 103), (404, 4, 104), (405, 4, 105),
    (406, 4, 106), (407, 4, 107), (408, 4, 108), (409, 4, 110), (410, 4, 111),
    -- SUPER_ADMIN (role_id = 5)：101 ~ 111 全部
    (501, 5, 101), (502, 5, 102), (503, 5, 103), (504, 5, 104), (505, 5, 105),
    (506, 5, 106), (507, 5, 107), (508, 5, 108), (509, 5, 109), (510, 5, 110),
    (511, 5, 111);
