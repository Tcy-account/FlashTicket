-- =====================================================================
-- FlashTicket M08 · 同步抢票请求与订单创建
-- 建表：ft_purchase_request / ft_order / ft_order_item
-- 依据：doc/订单与交易状态机模块.md（五、订单创建；八、交易快照；九、数据模型）
--        doc/抢票请求与异步排队模块.md（四、请求状态机；十一、实体规划）
--        doc/全局数据库模型与ER关系设计.md（七、抢票请求域；八、订单与交易域）
--        doc/跨域一致性与工程落地审查.md（C03 锁顺序；C07 金额口径；C08 幂等分层）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码、金额 BIGINT 分
-- 说明：M08 为同步抢票（不接 RocketMQ），「请求校验 → 限购占用 → 库存预占 →
--       创建订单」在同一本地事务完成。request_id 三重唯一（请求/预占/订单）兜底，
--       防止重复下单；order 保存金额与明细交易快照，不受后续票价配置变更影响。
--       订单状态机 PENDING_PAYMENT → PAID / CLOSED（关闭/取消在 M09 落地）。
-- =====================================================================

-- 1) 抢票请求（request_id 唯一幂等，一次用户抢票操作最多一条有效请求）
CREATE TABLE ft_purchase_request (
    id            BIGINT       NOT NULL COMMENT '雪花主键',
    request_id    VARCHAR(64)  NOT NULL COMMENT '抢票请求号（幂等键，唯一）',
    user_id       BIGINT       NOT NULL COMMENT '用户 ID',
    tier_id       BIGINT       NOT NULL COMMENT '票档 ID',
    quantity      INT          NOT NULL COMMENT '请求购买数量',
    status        VARCHAR(16)  NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED/SUCCEEDED/FAILED',
    fail_reason   VARCHAR(32)  NULL COMMENT '失败原因码（SOLD_OUT/LIMIT_EXCEEDED 等）',
    order_id      BIGINT       NULL COMMENT '成功生成的订单 ID（SUCCEEDED 时非空）',
    version       BIGINT       NOT NULL DEFAULT 0 COMMENT '状态变更版本',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_request (request_id),
    KEY idx_purchase_user (user_id, id),
    KEY idx_purchase_tier (tier_id, id),
    CONSTRAINT chk_pr_qty CHECK (quantity > 0),
    CONSTRAINT chk_pr_status CHECK (status IN ('QUEUED', 'SUCCEEDED', 'FAILED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='抢票请求';

-- 2) 订单主体（order_no 业务号唯一，request_id 唯一防重复下单）
CREATE TABLE ft_order (
    id                    BIGINT       NOT NULL COMMENT '雪花主键',
    order_no              VARCHAR(32)  NOT NULL COMMENT '业务订单号（用户可见，唯一）',
    request_id            VARCHAR(64)  NOT NULL COMMENT '抢票请求号（一个请求最多一个订单）',
    user_id               BIGINT       NOT NULL COMMENT '买家用户 ID',
    status                VARCHAR(24)  NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT 'PENDING_PAYMENT/PAID/CLOSED',
    original_amount_cent  BIGINT       NOT NULL COMMENT '优惠前金额（分，下单快照）',
    discount_amount_cent  BIGINT       NOT NULL DEFAULT 0 COMMENT '优惠减免金额（分）',
    payable_amount_cent   BIGINT       NOT NULL COMMENT '应付金额（分，= original - discount）',
    paid_amount_cent      BIGINT       NOT NULL DEFAULT 0 COMMENT '实际确认支付金额（分）',
    expire_at             DATETIME(3)  NOT NULL COMMENT '支付截止时间（订单创建时刻 + 15 分钟）',
    paid_at               DATETIME(3)  NULL COMMENT '支付成功时间',
    closed_at             DATETIME(3)  NULL COMMENT '关单时间',
    close_reason          VARCHAR(24)  NULL COMMENT '关单原因 USER_CANCEL/PAYMENT_TIMEOUT/EVENT_CANCELLED/SYSTEM_CANCEL',
    version               BIGINT       NOT NULL DEFAULT 0 COMMENT '订单状态并发版本',
    created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_order_request (request_id),
    KEY idx_order_user (user_id, created_at, id),
    KEY idx_order_status_expire (status, expire_at, id),
    CONSTRAINT chk_order_amount CHECK (original_amount_cent >= 0 AND discount_amount_cent >= 0
                                       AND payable_amount_cent >= 0 AND paid_amount_cent >= 0),
    CONSTRAINT chk_order_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CLOSED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='订单主体';

-- 3) 订单明细（不可变交易快照，首版一笔订单仅一个票档明细，预留多明细扩展）
CREATE TABLE ft_order_item (
    id                BIGINT       NOT NULL COMMENT '雪花主键',
    order_id          BIGINT       NOT NULL COMMENT '所属订单 ID',
    event_id          BIGINT       NOT NULL COMMENT '活动 ID',
    event_title       VARCHAR(128) NOT NULL COMMENT '下单时活动名称',
    session_id        BIGINT       NOT NULL COMMENT '场次 ID',
    session_start_at  DATETIME(3)  NOT NULL COMMENT '下单时演出开始时间',
    venue_name        VARCHAR(128) NOT NULL COMMENT '下单时场馆名称',
    tier_id           BIGINT       NOT NULL COMMENT '票档 ID',
    tier_name         VARCHAR(64)  NOT NULL COMMENT '下单时票档名称',
    unit_price_cent   BIGINT       NOT NULL COMMENT '下单时单价（分）',
    quantity          INT          NOT NULL COMMENT '购买数量',
    amount_cent       BIGINT       NOT NULL COMMENT '本项金额（分，= 单价 * 数量）',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id, id),
    CONSTRAINT chk_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_item_amount CHECK (unit_price_cent >= 0 AND amount_cent >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='订单明细交易快照';

-- 4) 订单域权限点种子（id 228，承接 M07 inventory:read 227）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (228, 'order:read', '订单查询', 'API', 'ACTIVE');

-- 5) 角色-权限关系种子
--    OPERATOR 管交易日常查看、FINANCE 管财务对账查看、SUPER_ADMIN 全量。
--    订单取消/人工关单（order:cancel/close）留待 M09 落地，M08 仅只读查询。
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- OPERATOR (role_id = 2)
    (731, 2, 228),
    -- FINANCE (role_id = 3)
    (732, 3, 228),
    -- SUPER_ADMIN (role_id = 5)
    (733, 5, 228);
