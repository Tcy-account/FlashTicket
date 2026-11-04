-- =====================================================================
-- FlashTicket M05 · 演出活动与场次管理
-- 建表：ft_event / ft_session
-- 依据：doc/全局数据库模型与ER关系设计.md（五、演出/场馆/场次/票档域）
--        doc/全局逻辑ER模型.md（三、ft_event；五、ft_session）
--        doc/演出活动与场次.md（三、活动管理；五、场次管理）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码
-- 说明：M05 只落地活动草稿生命周期（DRAFT/CANCELLED）与场次状态机
--       （SCHEDULED/POSTPONED/CANCELLED）；审核发布（PENDING_REVIEW/
--        REJECTED/PUBLISHED）及票档、售票计划留待 M06。
-- =====================================================================

-- 1) 演出活动主表
CREATE TABLE ft_event (
    id               BIGINT       NOT NULL COMMENT '雪花主键',
    category_id      BIGINT       NOT NULL COMMENT '分类 ID',
    title            VARCHAR(100) NOT NULL COMMENT '演出名称',
    subtitle         VARCHAR(200) NULL COMMENT '副标题',
    cover_url        VARCHAR(512) NULL COMMENT '封面地址',
    description      VARCHAR(500) NULL COMMENT '简介',
    detail_content   MEDIUMTEXT   NULL COMMENT '演出详细说明（富文本）',
    organizer_name   VARCHAR(100) NULL COMMENT '主办方名称',
    attendance_notice TEXT        NULL COMMENT '观演须知',
    refund_policy    TEXT        NULL COMMENT '退票规则描述',
    status           VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT '活动生命周期：DRAFT/PENDING_REVIEW/REJECTED/PUBLISHED/CANCELLED/ARCHIVED',
    published_at     DATETIME(3)  NULL COMMENT '最后一次发布生效时间',
    cancelled_at     DATETIME(3)  NULL COMMENT '取消时间',
    cancel_reason    VARCHAR(500) NULL COMMENT '取消原因',
    created_by       BIGINT       NOT NULL COMMENT '创建人',
    updated_by       BIGINT       NOT NULL COMMENT '最近修改人',
    version          BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观并发版本',
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_event_category_status (category_id, status, id),
    KEY idx_event_status_created (status, created_at, id),
    KEY idx_event_created_by (created_by, created_at),
    CONSTRAINT fk_event_category FOREIGN KEY (category_id) REFERENCES ft_event_category (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='演出活动';

-- 2) 演出场次表
CREATE TABLE ft_session (
    id             BIGINT       NOT NULL COMMENT '雪花主键',
    event_id       BIGINT       NOT NULL COMMENT '活动 ID',
    venue_id       BIGINT       NOT NULL COMMENT '场馆 ID',
    name           VARCHAR(64)  NOT NULL COMMENT '场次名称',
    start_at       DATETIME(3)  NOT NULL COMMENT '演出开始时间（UTC）',
    end_at         DATETIME(3)  NOT NULL COMMENT '预计结束时间（UTC）',
    entry_start_at DATETIME(3)  NULL COMMENT '开始入场时间（UTC）',
    sale_capacity  INT          NOT NULL COMMENT '场次核定可售量',
    status         VARCHAR(24)  NOT NULL DEFAULT 'SCHEDULED' COMMENT '场次状态：SCHEDULED/POSTPONED/CANCELLED/COMPLETED',
    postponed_at   DATETIME(3)  NULL COMMENT '延期记录时间',
    cancelled_at   DATETIME(3)  NULL COMMENT '取消时间',
    change_reason  VARCHAR(500) NULL COMMENT '延期或取消原因',
    version        BIGINT       NOT NULL DEFAULT 0 COMMENT '并发版本',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_session_event_start (event_id, start_at, id),
    KEY idx_session_venue_time (venue_id, start_at, end_at),
    KEY idx_session_status_start (status, start_at, id),
    CONSTRAINT fk_session_event FOREIGN KEY (event_id) REFERENCES ft_event (id),
    CONSTRAINT fk_session_venue FOREIGN KEY (venue_id) REFERENCES ft_venue (id),
    CONSTRAINT chk_session_time CHECK (end_at > start_at),
    CONSTRAINT chk_session_capacity CHECK (sale_capacity >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='演出场次';

-- 3) 演出域权限点种子（id 209 起，承接 M04 分类/场馆 201~208）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (209, 'event:event:list',       '活动列表',     'API', 'ACTIVE'),
    (210, 'event:event:create',     '创建活动',     'API', 'ACTIVE'),
    (211, 'event:event:update',     '编辑活动',     'API', 'ACTIVE'),
    (212, 'event:event:cancel',     '取消活动',     'API', 'ACTIVE'),
    (213, 'event:session:list',     '场次列表',     'API', 'ACTIVE'),
    (214, 'event:session:create',   '创建场次',     'API', 'ACTIVE'),
    (215, 'event:session:update',   '编辑场次',     'API', 'ACTIVE'),
    (216, 'event:session:postpone', '延期场次',     'API', 'ACTIVE'),
    (217, 'event:session:cancel',   '取消场次',     'API', 'ACTIVE');

-- 4) 角色-权限关系种子（见 doc/演出活动与场次.md 九）
--    OPERATOR：活动/场次日常运营（创建、编辑、延期）；取消属高风险归 SUPER_ADMIN
--    SUPER_ADMIN：演出域全量（含活动取消、场次取消）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- OPERATOR (role_id = 2)
    (609, 2, 209), (610, 2, 210), (611, 2, 211),
    (612, 2, 213), (613, 2, 214), (614, 2, 215), (615, 2, 216),
    -- SUPER_ADMIN (role_id = 5)
    (709, 5, 209), (710, 5, 210), (711, 5, 211), (712, 5, 212),
    (713, 5, 213), (714, 5, 214), (715, 5, 215), (716, 5, 216), (717, 5, 217);
