-- =====================================================================
-- FlashTicket M07 · MySQL 库存总账、预占、流水与用户限购
-- 建表：ft_inventory / ft_reservation / ft_inventory_ledger / ft_purchase_limit
-- 依据：doc/库存与票档预占模块.md（二、库存三态账本；五、数据库模型设计）
--        doc/全局数据库模型与ER关系设计.md（六、库存与预占域；七、抢票请求域）
--        doc/跨域一致性与工程落地审查.md（C03 统一锁顺序；C04 条件扣减兜底）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码
-- 说明：库存采用三态账本 total = available + reserved + sold，MySQL 条件更新
--       作为防超卖最终安全边界；预占状态机 RESERVED→CONFIRMED/RELEASED；
--       流水不可变；限购账本 UNIQUE(user_id, tier_id) 原子占用。
--       高频交易/流水表使用显式关联 ID + 唯一约束，不设物理外键（见 C10）。
-- =====================================================================

-- 1) 库存总账（一票档一账本，tier_id 即主键）
CREATE TABLE ft_inventory (
    tier_id        BIGINT       NOT NULL COMMENT '票档 ID（主键，一票档一账本）',
    total          INT          NOT NULL COMMENT '核定总票量',
    available      INT          NOT NULL COMMENT '可售票量',
    reserved       INT          NOT NULL COMMENT '已预占票量',
    sold           INT          NOT NULL COMMENT '已售票量（仍占用容量，不代表净有效票）',
    version        BIGINT       NOT NULL DEFAULT 0 COMMENT '账目变更版本，每次变化递增',
    generation     BIGINT       NOT NULL DEFAULT 1 COMMENT '销售代次（Redis 高速准入预留）',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (tier_id),
    CONSTRAINT chk_inv_total CHECK (total >= 0),
    CONSTRAINT chk_inv_available CHECK (available >= 0),
    CONSTRAINT chk_inv_reserved CHECK (reserved >= 0),
    CONSTRAINT chk_inv_sold CHECK (sold >= 0),
    CONSTRAINT chk_inv_conservation CHECK (total = available + reserved + sold)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='库存总账';

-- 2) 预占记录（request_id 幂等键，一次抢票请求最多一条有效预占）
CREATE TABLE ft_reservation (
    id             BIGINT       NOT NULL COMMENT '雪花主键',
    request_id     VARCHAR(64)  NOT NULL COMMENT '抢票请求号（幂等键，唯一）',
    tier_id        BIGINT       NOT NULL COMMENT '票档 ID',
    user_id        BIGINT       NOT NULL COMMENT '用户 ID',
    quantity       INT          NOT NULL COMMENT '预占票数',
    status         VARCHAR(16)  NOT NULL DEFAULT 'RESERVED' COMMENT 'RESERVED/CONFIRMED/RELEASED',
    expire_at      DATETIME(3)  NOT NULL COMMENT '预占过期时间（订单待支付期限）',
    version        BIGINT       NOT NULL DEFAULT 0 COMMENT '状态变更版本',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_request (request_id),
    KEY idx_reservation_tier_status_expire (tier_id, status, expire_at),
    KEY idx_reservation_user (user_id, id),
    CONSTRAINT chk_reservation_qty CHECK (quantity > 0),
    CONSTRAINT chk_reservation_status CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='库存预占记录';

-- 3) 库存流水（不可变账本，operation_id 幂等，承担故障审计与重建依据）
CREATE TABLE ft_inventory_ledger (
    id             BIGINT       NOT NULL COMMENT '雪花主键',
    operation_id   VARCHAR(64)  NOT NULL COMMENT '操作幂等 ID（唯一）',
    tier_id        BIGINT       NOT NULL COMMENT '票档 ID',
    reservation_id BIGINT       NULL COMMENT '关联预占记录（初始化等无预占的操作可为空）',
    operation      VARCHAR(24)  NOT NULL COMMENT 'INIT/RESERVE/CONFIRM/RELEASE',
    quantity       INT          NOT NULL COMMENT '本次变化票数（非负，方向由 operation 决定）',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ledger_operation (operation_id),
    KEY idx_ledger_tier (tier_id, id),
    KEY idx_ledger_reservation (reservation_id, id),
    CONSTRAINT chk_ledger_qty CHECK (quantity >= 0),
    CONSTRAINT chk_ledger_operation CHECK (operation IN ('INIT', 'RESERVE', 'CONFIRM', 'RELEASE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='库存变化流水';

-- 4) 用户限购账本（UNIQUE(user_id, tier_id)，原子占用限购名额）
CREATE TABLE ft_purchase_limit (
    id                BIGINT       NOT NULL COMMENT '雪花主键',
    user_id           BIGINT       NOT NULL COMMENT '用户 ID',
    tier_id           BIGINT       NOT NULL COMMENT '票档 ID',
    reserved_quantity INT          NOT NULL DEFAULT 0 COMMENT '预占中数量',
    sold_quantity     INT          NOT NULL DEFAULT 0 COMMENT '已购数量',
    version           BIGINT       NOT NULL DEFAULT 0 COMMENT '账目变更版本',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_limit_user_tier (user_id, tier_id),
    CONSTRAINT chk_pl_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_pl_sold CHECK (sold_quantity >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户限购账本';

-- 5) 库存域权限点种子（id 227 起，承接 M06 票档/审核 218~226）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (227, 'inventory:read', '库存查询', 'API', 'ACTIVE');

-- 6) 角色-权限关系种子（OPERATOR 管库存日常查看，SUPER_ADMIN 全量）
--    库存调整/冻结/对账等高风险操作（inventory:adjust/manage/reconcile）
--    留待 M16 统计对账与补偿阶段落地，M07 仅开放只读查询。
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- OPERATOR (role_id = 2)
    (727, 2, 227),
    -- SUPER_ADMIN (role_id = 5)
    (728, 5, 227);
