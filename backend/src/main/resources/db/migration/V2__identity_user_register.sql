-- =====================================================================
-- FlashTicket M01 · 用户注册与邮箱验证
-- 建表：ft_user / ft_user_profile / ft_role / ft_user_role
-- 依据：doc/全局数据库模型与ER关系设计.md（四、用户与权限域）
--        doc/用户与账号模块设计.md（十一、数据模型规划）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码
-- 说明：ft_permission / ft_role_permission 属 RBAC，留待 M03 落地。
-- =====================================================================

-- 1) 用户身份主体
CREATE TABLE ft_user (
    id            BIGINT       NOT NULL COMMENT '雪花主键',
    username      VARCHAR(32)  NOT NULL COMMENT '用户名，4-32 字符，唯一（不区分大小写）',
    email         VARCHAR(254) NOT NULL COMMENT '邮箱，规范化后全局唯一',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '账号主状态：ACTIVE/DISABLED/DELETION_PENDING/DELETED',
    last_login_at DATETIME(3)  NULL COMMENT '最后登录时间（UTC）',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户身份主体';

-- 2) 用户资料（与用户一对一）
CREATE TABLE ft_user_profile (
    id         BIGINT       NOT NULL COMMENT '雪花主键',
    user_id    BIGINT       NOT NULL COMMENT '所属用户（一对一）',
    nickname   VARCHAR(32)  NOT NULL COMMENT '昵称，2-32 字符',
    avatar_url VARCHAR(255) NULL COMMENT '头像地址，默认系统头像',
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES ft_user (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户资料';

-- 3) 角色定义
CREATE TABLE ft_role (
    id         BIGINT      NOT NULL COMMENT '雪花主键',
    code       VARCHAR(64) NOT NULL COMMENT '角色码，全局唯一：USER/OPERATOR/FINANCE/ADMIN/SUPER_ADMIN',
    name       VARCHAR(64) NOT NULL COMMENT '角色显示名',
    status     VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '角色状态：ACTIVE/DISABLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色定义';

-- 4) 用户与角色多对多关系
CREATE TABLE ft_user_role (
    id         BIGINT      NOT NULL COMMENT '雪花主键',
    user_id    BIGINT      NOT NULL COMMENT '用户',
    role_id    BIGINT      NOT NULL COMMENT '角色',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES ft_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES ft_role (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户与角色关系';

-- 5) 五种正式角色种子数据（固定 id 便于关系引用）
INSERT INTO ft_role (id, code, name, status) VALUES
    (1, 'USER',        '普通用户',   'ACTIVE'),
    (2, 'OPERATOR',    '运营人员',   'ACTIVE'),
    (3, 'FINANCE',     '财务人员',   'ACTIVE'),
    (4, 'ADMIN',       '系统管理员', 'ACTIVE'),
    (5, 'SUPER_ADMIN', '超级管理员', 'ACTIVE');
