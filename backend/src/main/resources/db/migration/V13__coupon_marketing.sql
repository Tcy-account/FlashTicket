-- V13：优惠券与营销活动模块（M13）
-- 设计依据：doc/优惠券与营销活动模块.md
-- 三级概念：营销活动 Campaign → 优惠券模板 Coupon Template → 用户优惠券 User Coupon
-- 发行额度三态账本：total = available + reserved + issued（reserved 为异步发券预占预留）
-- 第一版 MySQL 同步领券为正确性基线，reserved 恒为 0；后续 MQ 异步发券时启用预占。

-- 1) 营销活动主体
CREATE TABLE ft_campaign (
    id          BIGINT       NOT NULL COMMENT '雪花主键',
    name        VARCHAR(100) NOT NULL COMMENT '活动名称',
    type        VARCHAR(32)  NOT NULL COMMENT 'FLASH_CLAIM/NEW_USER/EVENT_EXCLUSIVE',
    description VARCHAR(500) NULL COMMENT '活动说明',
    start_at    DATETIME(3)  NOT NULL COMMENT '活动开始时间（UTC）',
    end_at      DATETIME(3)  NOT NULL COMMENT '活动结束时间（UTC）',
    status      VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_REVIEW/PUBLISHED/ENDED/CANCELLED',
    created_by  BIGINT       NOT NULL COMMENT '创建人（运营）',
    reviewed_by BIGINT       NULL COMMENT '审核人（须 != 创建人）',
    reviewed_at DATETIME(3)  NULL COMMENT '审核时间',
    version     BIGINT       NOT NULL DEFAULT 0 COMMENT '配置并发版本',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_campaign_status (status, start_at, end_at),
    CONSTRAINT chk_campaign_time CHECK (end_at > start_at),
    CONSTRAINT chk_campaign_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'ENDED', 'CANCELLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='营销活动';

-- 2) 优惠券模板（规则发布后关键字段冻结，靠 version 快照）
CREATE TABLE ft_coupon_template (
    id                BIGINT       NOT NULL COMMENT '雪花主键',
    campaign_id       BIGINT       NOT NULL COMMENT '关联营销活动',
    name              VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type              VARCHAR(32)  NOT NULL COMMENT 'FULL_REDUCTION(满减)/PERCENTAGE(折扣)',
    threshold_cent    BIGINT       NOT NULL DEFAULT 0 COMMENT '最低消费金额（分）',
    discount_cent     BIGINT       NOT NULL DEFAULT 0 COMMENT '满减金额（分，FULL_REDUCTION）',
    discount_rate     INT          NOT NULL DEFAULT 0 COMMENT '折扣基点（9000=9折，PERCENTAGE）',
    max_discount_cent BIGINT       NOT NULL DEFAULT 0 COMMENT '最高优惠金额（分，PERCENTAGE）',
    total_quantity    INT          NOT NULL COMMENT '最大发行总量',
    per_user_limit    INT          NOT NULL DEFAULT 1 COMMENT '每用户领取上限',
    claim_start_at    DATETIME(3)  NOT NULL COMMENT '领取窗口开始（UTC）',
    claim_end_at      DATETIME(3)  NOT NULL COMMENT '领取窗口结束（UTC）',
    validity_type     VARCHAR(24)  NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED(固定时间)/RELATIVE_DAYS(领取后N天)',
    valid_from        DATETIME(3)  NULL COMMENT '固定有效期起（FIXED）',
    valid_until       DATETIME(3)  NULL COMMENT '固定有效期止（FIXED）',
    valid_days        INT          NULL COMMENT '领取后 N 天有效（RELATIVE_DAYS）',
    applicable_scope  VARCHAR(24)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL(全平台)/EVENT(指定活动)/TIER(指定票档)',
    scope_ref_id      BIGINT       NULL COMMENT 'EVENT/TIER 时的事件或票档 ID',
    status            VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/PAUSED/TERMINATED',
    version           BIGINT       NOT NULL DEFAULT 0 COMMENT '配置版本',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_template_campaign (campaign_id),
    KEY idx_template_status (status, claim_start_at, claim_end_at),
    CONSTRAINT chk_template_qty CHECK (total_quantity > 0 AND per_user_limit > 0),
    CONSTRAINT chk_template_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'TERMINATED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='优惠券模板';

-- 3) 模板发行额度账本（template_id 唯一，三态守恒，防超发最终边界）
CREATE TABLE ft_coupon_quota (
    id          BIGINT      NOT NULL COMMENT '雪花主键',
    template_id BIGINT      NOT NULL COMMENT '优惠券模板 ID',
    total       INT         NOT NULL COMMENT '核定发行总量',
    available   INT         NOT NULL COMMENT '剩余可发放',
    reserved    INT         NOT NULL DEFAULT 0 COMMENT '已取得资格未落库（异步预占）',
    issued      INT         NOT NULL DEFAULT 0 COMMENT '已成功发放',
    version     BIGINT      NOT NULL DEFAULT 0 COMMENT '并发版本',
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_quota_template (template_id),
    CONSTRAINT chk_quota_balance CHECK (total = available + reserved + issued
                                        AND available >= 0 AND reserved >= 0 AND issued >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='优惠券发行额度账本';

-- 4) 用户限领账本（FOR UPDATE 锁行，防并发绕过限领）
CREATE TABLE ft_coupon_user_quota (
    id            BIGINT      NOT NULL COMMENT '雪花主键',
    user_id       BIGINT      NOT NULL COMMENT '用户 ID',
    template_id   BIGINT      NOT NULL COMMENT '优惠券模板 ID',
    claimed_count INT         NOT NULL DEFAULT 0 COMMENT '已领取数量',
    version       BIGINT      NOT NULL DEFAULT 0 COMMENT '并发版本',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_quota (user_id, template_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户领取额度账本';

-- 5) 领券请求（request_id 唯一幂等，六态预留异步发券）
CREATE TABLE ft_coupon_claim_request (
    id             BIGINT      NOT NULL COMMENT '雪花主键',
    request_id     VARCHAR(64) NOT NULL COMMENT '领券请求号（幂等键，唯一）',
    user_id        BIGINT      NOT NULL COMMENT '用户 ID',
    template_id    BIGINT      NOT NULL COMMENT '优惠券模板 ID',
    status         VARCHAR(16) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED/PROCESSING/RECOVERING/SUCCEEDED/FAILED/CANCELLED',
    fail_reason    VARCHAR(32) NULL COMMENT '失败原因码',
    user_coupon_id BIGINT      NULL COMMENT '发放成功的用户券 ID（SUCCEEDED 时非空）',
    version        BIGINT      NOT NULL DEFAULT 0 COMMENT '并发版本',
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_claim_request (request_id),
    KEY idx_claim_user (user_id, id),
    KEY idx_claim_template (template_id, id),
    CONSTRAINT chk_claim_status CHECK (status IN ('QUEUED', 'PROCESSING', 'RECOVERING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='领券请求';

-- 6) 用户优惠券（用户持有的券，状态机 AVAILABLE→LOCKED→USED/AVAILABLE）
CREATE TABLE ft_user_coupon (
    id          BIGINT      NOT NULL COMMENT '雪花主键',
    user_id     BIGINT      NOT NULL COMMENT '用户 ID',
    template_id BIGINT      NOT NULL COMMENT '优惠券模板 ID',
    campaign_id BIGINT      NOT NULL COMMENT '来源营销活动 ID',
    coupon_no   VARCHAR(32) NOT NULL COMMENT '券号（用户可见，唯一）',
    status      VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE/LOCKED/USED/EXPIRED/VOID',
    valid_from  DATETIME(3) NOT NULL COMMENT '有效期开始（UTC）',
    valid_until DATETIME(3) NOT NULL COMMENT '有效期结束（UTC）',
    locked_at   DATETIME(3) NULL COMMENT '锁定时间',
    used_at     DATETIME(3) NULL COMMENT '核销时间',
    expired_at  DATETIME(3) NULL COMMENT '过期时间',
    voided_at   DATETIME(3) NULL COMMENT '作废时间',
    version     BIGINT      NOT NULL DEFAULT 0 COMMENT '并发版本',
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_no (coupon_no),
    KEY idx_uc_user_status (user_id, status, valid_until),
    KEY idx_uc_template (template_id),
    CONSTRAINT chk_uc_status CHECK (status IN ('AVAILABLE', 'LOCKED', 'USED', 'EXPIRED', 'VOID'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户优惠券';

-- 7) 订单锁券与核销记录（同一券同一订单唯一；锁券原子性由 user_coupon 条件更新保证）
CREATE TABLE ft_coupon_usage (
    id             BIGINT      NOT NULL COMMENT '雪花主键',
    user_coupon_id BIGINT      NOT NULL COMMENT '用户优惠券 ID',
    order_id       BIGINT      NOT NULL COMMENT '订单 ID',
    status         VARCHAR(16) NOT NULL DEFAULT 'LOCKED' COMMENT 'LOCKED/USED/RELEASED',
    locked_at      DATETIME(3) NULL COMMENT '锁定时间',
    used_at        DATETIME(3) NULL COMMENT '核销时间',
    released_at    DATETIME(3) NULL COMMENT '释放时间',
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_usage_coupon_order (user_coupon_id, order_id),
    KEY idx_usage_order (order_id),
    CONSTRAINT chk_usage_status CHECK (status IN ('LOCKED', 'USED', 'RELEASED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='优惠券锁券核销记录';

-- 8) 优惠券审计流水（operation_id 唯一，额度调整/发券/锁券/核销/返券/过期/作废）
CREATE TABLE ft_coupon_ledger (
    id             BIGINT       NOT NULL COMMENT '雪花主键',
    operation_id   VARCHAR(64)  NOT NULL COMMENT '操作流水号（唯一）',
    user_coupon_id BIGINT       NULL COMMENT '用户优惠券 ID',
    template_id    BIGINT       NOT NULL COMMENT '优惠券模板 ID',
    user_id        BIGINT       NULL COMMENT '用户 ID',
    order_id       BIGINT       NULL COMMENT '关联订单 ID',
    operation      VARCHAR(32)  NOT NULL COMMENT 'QUOTA_ADJUST/CLAIM/LOCK/USE/RELEASE/EXPIRE/VOID',
    amount         INT          NOT NULL DEFAULT 0 COMMENT '额度变化量（发券 -1 等）',
    remark         VARCHAR(200) NULL COMMENT '备注',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ledger_operation (operation_id),
    KEY idx_ledger_template (template_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='优惠券审计流水';

-- 9) ft_order 扩展优惠字段（下单时快照，支付/退款依据）
ALTER TABLE ft_order
    ADD COLUMN user_coupon_id      BIGINT        NULL COMMENT '使用的用户优惠券 ID' AFTER discount_amount_cent,
    ADD COLUMN coupon_template_id  BIGINT        NULL COMMENT '优惠券模板 ID' AFTER user_coupon_id,
    ADD COLUMN promotion_snapshot  VARCHAR(1000) NULL COMMENT '优惠规则快照（JSON）' AFTER coupon_template_id;

-- 10) ft_purchase_request 扩展选中券（抢票时选择，建单时正式锁券）
ALTER TABLE ft_purchase_request
    ADD COLUMN selected_coupon_id BIGINT NULL COMMENT '抢票时选中的用户优惠券 ID' AFTER quantity;

-- 11) 营销域权限点种子（id 235 起，承接 M12 purchase:retry 234）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (235, 'campaign:manage', '营销活动管理', 'API', 'ACTIVE'),
    (236, 'campaign:review', '营销活动审核', 'API', 'ACTIVE'),
    (237, 'coupon:manage', '优惠券管理', 'API', 'ACTIVE'),
    (238, 'coupon:read', '优惠券查询', 'API', 'ACTIVE'),
    (239, 'coupon:reconcile', '优惠券对账', 'API', 'ACTIVE');

-- 12) 角色-权限关系（OPERATOR 管理，FINANCE 查/对账，SUPER_ADMIN 全+审核）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- campaign:manage (235) → OPERATOR(2), SUPER_ADMIN(5)
    (746, 2, 235), (747, 5, 235),
    -- campaign:review (236) → SUPER_ADMIN(5)
    (748, 5, 236),
    -- coupon:manage (237) → OPERATOR(2), SUPER_ADMIN(5)
    (749, 2, 237), (750, 5, 237),
    -- coupon:read (238) → OPERATOR(2), FINANCE(3), SUPER_ADMIN(5)
    (751, 2, 238), (752, 3, 238), (753, 5, 238),
    -- coupon:reconcile (239) → FINANCE(3), SUPER_ADMIN(5)
    (754, 3, 239), (755, 5, 239);
