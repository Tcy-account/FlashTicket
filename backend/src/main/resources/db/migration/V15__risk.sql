-- V15：风险控制与防刷（M15-B）
-- 设计依据：doc/风险控制与防刷模块.md
-- P0 聚焦「可管理和审计的风险限制」：临时限制（黑名单）+ 风控规则 + 操作审计。
-- 风险事件/决策（ft_risk_event/ft_risk_decision）与规则版本快照留 P1，避免
-- 大量低价值事件拖慢核心交易事务。

-- 1) 临时限制/黑名单（subject_type+subject_id 定位主体，scope 限定业务范围）
CREATE TABLE ft_risk_restriction (
    id           BIGINT       NOT NULL COMMENT '雪花主键',
    subject_type VARCHAR(16)  NOT NULL COMMENT 'USER/IP',
    subject_id   VARCHAR(64)  NOT NULL COMMENT '主体标识（userId 或 IP）',
    scope        VARCHAR(16)  NOT NULL COMMENT 'PURCHASE/CLAIM/LOGIN/ALL',
    reason       VARCHAR(200) NOT NULL COMMENT '限制原因（用户可见）',
    expire_at    DATETIME(3)  NOT NULL COMMENT '到期时间（到期自动解除）',
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/REVOKED',
    created_by   BIGINT       NOT NULL COMMENT '创建人（管理员）',
    revoked_by   BIGINT       NULL COMMENT '提前解除人',
    revoked_at   DATETIME(3)  NULL COMMENT '提前解除时间',
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_restriction_subject (subject_type, subject_id, status),
    KEY idx_restriction_expire (expire_at, status),
    CONSTRAINT chk_restriction_type CHECK (subject_type IN ('USER', 'IP')),
    CONSTRAINT chk_restriction_scope CHECK (scope IN ('PURCHASE', 'CLAIM', 'LOGIN', 'ALL')),
    CONSTRAINT chk_restriction_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='风控临时限制/黑名单';

-- 2) 风控规则（P0 确定性规则：场景 + 动作 + JSON 配置，启停由管理员控制）
CREATE TABLE ft_risk_rule (
    id         BIGINT        NOT NULL COMMENT '雪花主键',
    rule_code  VARCHAR(64)   NOT NULL COMMENT '规则唯一码',
    name       VARCHAR(100)  NOT NULL COMMENT '规则名称',
    scene      VARCHAR(32)   NOT NULL COMMENT 'PURCHASE/CLAIM/LOGIN/QUERY',
    action     VARCHAR(16)   NOT NULL COMMENT 'THROTTLE/DENY',
    config     VARCHAR(1000) NULL COMMENT 'JSON 配置（阈值等，按场景解释）',
    priority   INT           NOT NULL DEFAULT 0 COMMENT '优先级（数值越大越先执行）',
    status     VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    version    BIGINT        NOT NULL DEFAULT 0 COMMENT '并发版本',
    created_by BIGINT        NOT NULL COMMENT '创建人',
    created_at DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code),
    CONSTRAINT chk_rule_action CHECK (action IN ('THROTTLE', 'DENY')),
    CONSTRAINT chk_rule_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='风控规则';

-- 3) 风控操作审计（管理员创建/解除限制、启停规则等关键变更）
CREATE TABLE ft_risk_operation (
    id          BIGINT       NOT NULL COMMENT '雪花主键',
    operator_id BIGINT       NOT NULL COMMENT '操作人',
    target_type VARCHAR(16)  NOT NULL COMMENT 'RESTRICTION/RULE',
    target_id   VARCHAR(64)  NOT NULL COMMENT '目标 ID（雪花字符串）',
    action      VARCHAR(32)  NOT NULL COMMENT 'CREATE/REVOKE/UPDATE/ENABLE/DISABLE',
    reason      VARCHAR(200) NULL COMMENT '操作原因/备注',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_risk_op_operator (operator_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='风控操作审计';

-- 4) 风控域权限点种子（id 243 起，承接 M14 notification:manage 242）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (243, 'risk:read', '风控查询', 'API', 'ACTIVE'),
    (244, 'risk:rule:manage', '风控规则管理', 'API', 'ACTIVE'),
    (245, 'risk:restriction:manage', '风控限制管理', 'API', 'ACTIVE'),
    (246, 'risk:admission:manage', '准入状态管理', 'API', 'ACTIVE'),
    (247, 'risk:audit:read', '风控审计查询', 'API', 'ACTIVE');

-- 5) 角色-权限关系（SUPER_ADMIN 管理规则与黑名单，OPERATOR 查询）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- risk:read (243) → OPERATOR(2), SUPER_ADMIN(5)
    (762, 2, 243), (763, 5, 243),
    -- risk:rule:manage (244) → SUPER_ADMIN(5)
    (764, 5, 244),
    -- risk:restriction:manage (245) → SUPER_ADMIN(5)
    (765, 5, 245),
    -- risk:admission:manage (246) → SUPER_ADMIN(5)
    (766, 5, 246),
    -- risk:audit:read (247) → OPERATOR(2), SUPER_ADMIN(5)
    (767, 2, 247), (768, 5, 247);
