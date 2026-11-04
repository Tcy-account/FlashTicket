-- =====================================================================
-- FlashTicket M04 · 演出分类与场馆管理
-- 建表：ft_event_category / ft_venue
-- 依据：doc/全局数据库模型与ER关系设计.md（五、演出/场馆/场次/票档域）
--        doc/全局逻辑ER模型.md（二、ft_event_category；四、ft_venue）
--        doc/演出活动与场次.md（四、场馆管理）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码
-- 说明：分类/场馆生命周期用 ENABLED/DISABLED 状态管理（停用不物理删除，
--       被活动/场次引用后更不允许物理删除，删除与引用校验留待 M05/M06）。
-- =====================================================================

-- 1) 演出分类表
CREATE TABLE ft_event_category (
    id          BIGINT       NOT NULL COMMENT '雪花主键',
    code        VARCHAR(32)  NOT NULL COMMENT '分类标识，稳定业务码',
    name        VARCHAR(64)  NOT NULL COMMENT '分类名称',
    description VARCHAR(255) NULL COMMENT '分类说明',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序权重，越小越靠前',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '是否启用：ENABLED/DISABLED',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_category_code (code),
    KEY idx_event_category_status_sort (status, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='演出分类';

-- 2) 演出场馆表
CREATE TABLE ft_venue (
    id           BIGINT        NOT NULL COMMENT '雪花主键',
    name         VARCHAR(100)  NOT NULL COMMENT '场馆名称',
    country_code CHAR(2)       NOT NULL DEFAULT 'CN' COMMENT 'ISO 国家代码',
    province     VARCHAR(64)   NULL COMMENT '省份',
    city         VARCHAR(64)   NOT NULL COMMENT '城市',
    district     VARCHAR(64)   NULL COMMENT '区县',
    address      VARCHAR(255)  NOT NULL COMMENT '详细地址',
    timezone     VARCHAR(64)   NOT NULL DEFAULT 'Asia/Shanghai' COMMENT 'IANA 时区',
    max_capacity INT           NOT NULL COMMENT '最大核定容量（人）',
    description  VARCHAR(1000) NULL COMMENT '场馆简介',
    status       VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT '场馆状态：ENABLED/DISABLED',
    version      BIGINT        NOT NULL DEFAULT 0 COMMENT '配置并发版本',
    created_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_venue_city_status (city, status, id),
    KEY idx_venue_name (name),
    CONSTRAINT chk_venue_capacity CHECK (max_capacity > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='演出场馆';

-- 3) 演出分类种子（固定 id 便于后续 ft_event.category_id 引用）
INSERT INTO ft_event_category (id, code, name, description, sort_order, status) VALUES
    (1, 'CONCERT',        '演唱会',   '歌手、乐队现场演唱会',      10, 'ENABLED'),
    (2, 'MUSIC_FESTIVAL', '音乐节',   '多组艺人联袂的音乐节',      20, 'ENABLED'),
    (3, 'THEATER',        '话剧演出', '话剧、舞台剧、音乐剧',      30, 'ENABLED'),
    (4, 'SPORTS',         '体育赛事', '竞技体育赛事',              40, 'ENABLED'),
    (5, 'COMEDY',         '脱口秀',   '脱口秀、相声等语言类演出',  50, 'ENABLED'),
    (6, 'EXHIBITION',     '展览活动', '艺术展、主题展等展览活动',  60, 'ENABLED');

-- 4) 演出域权限点种子（id 201 起，与 M03 身份域 101~111 区分）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (201, 'event:category:list',   '分类列表',     'API', 'ACTIVE'),
    (202, 'event:category:create', '创建分类',     'API', 'ACTIVE'),
    (203, 'event:category:update', '编辑分类',     'API', 'ACTIVE'),
    (204, 'event:category:toggle', '启用/停用分类', 'API', 'ACTIVE'),
    (205, 'event:venue:list',      '场馆列表',     'API', 'ACTIVE'),
    (206, 'event:venue:create',    '创建场馆',     'API', 'ACTIVE'),
    (207, 'event:venue:update',    '编辑场馆',     'API', 'ACTIVE'),
    (208, 'event:venue:toggle',    '启用/停用场馆', 'API', 'ACTIVE');

-- 5) 角色-权限关系种子：演出域由 OPERATOR 运营，SUPER_ADMIN 全权（见 doc/演出活动与场次.md 九）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- OPERATOR (role_id = 2)：分类/场馆管理全套
    (601, 2, 201), (602, 2, 202), (603, 2, 203), (604, 2, 204),
    (605, 2, 205), (606, 2, 206), (607, 2, 207), (608, 2, 208),
    -- SUPER_ADMIN (role_id = 5)：演出域全部
    (701, 5, 201), (702, 5, 202), (703, 5, 203), (704, 5, 204),
    (705, 5, 205), (706, 5, 206), (707, 5, 207), (708, 5, 208);
