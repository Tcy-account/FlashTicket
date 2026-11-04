-- =====================================================================
-- FlashTicket M06 · 票档、售票计划与审核发布
-- 建表：ft_ticket_tier / ft_sale_plan / ft_event_review
-- 依据：doc/全局数据库模型与ER关系设计.md（五、演出/场馆/场次/票档域）
--        doc/演出活动与场次.md（六、票档管理；七、售票计划；三、审核发布）
--        doc/跨域一致性与工程落地审查.md（C05：审核配置指纹与冻结规则）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码
-- 说明：票档管「卖什么、卖多少、限购多少」；售票计划一对一绑定票档，
--       管「何时卖、是否暂停」，sale_version 供后续抢票/库存模块校验过期
--       配置（防旧缓存放行）。审核发布引入整体配置指纹 snapshot_hash。
-- =====================================================================

-- 1) 票档表（数量型票，不指定座位）
CREATE TABLE ft_ticket_tier (
    id                BIGINT       NOT NULL COMMENT '雪花主键',
    session_id        BIGINT       NOT NULL COMMENT '所属场次 ID',
    name              VARCHAR(64)  NOT NULL COMMENT '票档名称（同场次内唯一）',
    tier_type         VARCHAR(24)  NOT NULL DEFAULT 'GENERAL_ADMISSION' COMMENT '票档类型，V1 仅通用入场',
    unit_price_cent   BIGINT       NOT NULL COMMENT '单价（分），支持 0 免费票',
    initial_quantity  INT          NOT NULL COMMENT '初始总库存（非负）',
    min_purchase      INT          NOT NULL DEFAULT 1 COMMENT '每单最小购买数',
    max_purchase      INT          NOT NULL DEFAULT 2 COMMENT '每单最大购买数',
    per_user_limit    INT          NOT NULL DEFAULT 2 COMMENT '每用户限购数',
    description       VARCHAR(500) NULL COMMENT '票档说明（区域、入场规则）',
    sort_order        INT          NOT NULL DEFAULT 0 COMMENT '排序权重，越小越靠前',
    version           BIGINT       NOT NULL DEFAULT 0 COMMENT '配置并发版本',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tier_session_name (session_id, name),
    KEY idx_tier_session (session_id, sort_order, id),
    CONSTRAINT fk_tier_session FOREIGN KEY (session_id) REFERENCES ft_session (id),
    CONSTRAINT chk_tier_price CHECK (unit_price_cent >= 0),
    CONSTRAINT chk_tier_quantity CHECK (initial_quantity >= 0),
    CONSTRAINT chk_tier_min CHECK (min_purchase >= 1),
    CONSTRAINT chk_tier_max CHECK (max_purchase >= min_purchase),
    CONSTRAINT chk_tier_user_limit CHECK (per_user_limit >= 1)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='场次票档';

-- 2) 售票计划表（一对一绑定票档）
CREATE TABLE ft_sale_plan (
    id             BIGINT       NOT NULL COMMENT '雪花主键',
    tier_id        BIGINT       NOT NULL COMMENT '票档 ID（唯一）',
    sale_start_at  DATETIME(3)  NOT NULL COMMENT '开售时间（UTC）',
    sale_end_at    DATETIME(3)  NOT NULL COMMENT '售票结束时间（UTC）',
    sale_status    VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '运营控制：ENABLED/PAUSED',
    sale_version   BIGINT       NOT NULL DEFAULT 0 COMMENT '销售配置版本，每次变更递增',
    paused_at      DATETIME(3)  NULL COMMENT '最近一次暂停时间',
    pause_reason   VARCHAR(500) NULL COMMENT '暂停原因',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sale_plan_tier (tier_id),
    CONSTRAINT fk_sale_plan_tier FOREIGN KEY (tier_id) REFERENCES ft_ticket_tier (id),
    CONSTRAINT chk_sale_plan_time CHECK (sale_end_at > sale_start_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='售票计划';

-- 3) 活动审核记录表（含整体配置指纹，见跨域审查 C05）
CREATE TABLE ft_event_review (
    id                     BIGINT       NOT NULL COMMENT '雪花主键',
    event_id               BIGINT       NOT NULL COMMENT '活动 ID',
    submitted_by           BIGINT       NOT NULL COMMENT '提交审核人（审核人不能审核自己提交的活动）',
    event_version          BIGINT       NOT NULL COMMENT '提交审核时的活动版本',
    snapshot_hash          CHAR(64)     NOT NULL COMMENT '审核范围规范化 SHA-256 摘要',
    snapshot_schema_version INT         NOT NULL DEFAULT 1 COMMENT '快照结构版本',
    review_status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    reviewer_id            BIGINT       NULL COMMENT '审核人',
    review_comment         VARCHAR(500) NULL COMMENT '审核意见（拒绝时必填）',
    reviewed_at            DATETIME(3)  NULL COMMENT '审核时间',
    created_at             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_review_event (event_id, id),
    KEY idx_review_status (review_status, id),
    CONSTRAINT fk_review_event FOREIGN KEY (event_id) REFERENCES ft_event (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='活动审核记录';

-- 4) 演出域权限点种子（id 218 起，承接 M05 活动/场次 209~217）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (218, 'event:tier:list',       '票档列表',       'API', 'ACTIVE'),
    (219, 'event:tier:create',     '创建票档',       'API', 'ACTIVE'),
    (220, 'event:tier:update',     '编辑票档',       'API', 'ACTIVE'),
    (221, 'event:sale-plan:update','配置售票计划',   'API', 'ACTIVE'),
    (222, 'event:sale-plan:pause', '暂停销售',       'API', 'ACTIVE'),
    (223, 'event:sale-plan:resume','恢复销售',       'API', 'ACTIVE'),
    (224, 'event:event:submit',    '提交活动审核',   'API', 'ACTIVE'),
    (225, 'event:event:review',    '审核活动',       'API', 'ACTIVE'),
    (226, 'event:event:publish',   '发布活动',       'API', 'ACTIVE');

-- 5) 角色-权限关系种子（见 doc/演出活动与场次.md 九）
--    OPERATOR：票档配置、售票计划、暂停/恢复、提交审核
--    SUPER_ADMIN：演出域全量（含审核、发布）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- OPERATOR (role_id = 2)
    (616, 2, 218), (617, 2, 219), (618, 2, 220),
    (619, 2, 221), (620, 2, 222), (621, 2, 223), (622, 2, 224),
    -- SUPER_ADMIN (role_id = 5)
    (718, 5, 218), (719, 5, 219), (720, 5, 220),
    (721, 5, 221), (722, 5, 222), (723, 5, 223), (724, 5, 224),
    (725, 5, 225), (726, 5, 226);
