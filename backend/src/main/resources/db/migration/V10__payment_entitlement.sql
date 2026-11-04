-- =====================================================================
-- FlashTicket M10 · 模拟支付与票务权益
-- 建表：ft_payment / ft_payment_attempt / ft_payment_callback / ft_ticket_entitlement
-- 依据：doc/模拟支付与退款模块.md（八、数据模型；九、接口）
--        doc/全局逻辑ER模型.md（六、模拟支付与退款域 1925 行起）
--        doc/全局数据库模型与ER关系设计.md（九、模拟支付与退款域）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码、金额 BIGINT 分
-- 说明：M10 采用内部 MOCK_WALLET 模拟渠道（同步执行、不接 RocketMQ），实现
--       支付业务单与订单分离、支付尝试幂等、回调去重，以及「支付成功」与
--       「超时关单」在同一 MySQL 本地事务内争夺订单状态转换权（见订单状态机七）。
--       支付确认成功时：订单 PENDING_PAYMENT → PAID、库存 reserved → sold，
--       并为购买数量生成对应数量的有效票务权益（一票一权益）。
-- =====================================================================

-- 1) 支付业务单（一个订单在 V1 中只有一个支付业务单，允许多次受控尝试）
CREATE TABLE ft_payment (
    id                     BIGINT       NOT NULL COMMENT '雪花主键',
    payment_no             VARCHAR(40)  NOT NULL COMMENT '支付业务编号（幂等，唯一）',
    order_id               BIGINT       NOT NULL COMMENT '所属订单 ID',
    user_id                BIGINT       NOT NULL COMMENT '支付用户 ID',
    channel                VARCHAR(24)  NOT NULL DEFAULT 'MOCK_WALLET' COMMENT '模拟渠道',
    amount_cent            BIGINT       NOT NULL COMMENT '应付金额（分，= 订单 payable_amount_cent）',
    currency               CHAR(3)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status                 VARCHAR(24)  NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PROCESSING/SUCCEEDED/FAILED/CLOSED',
    expire_at              DATETIME(3)  NOT NULL COMMENT '支付截止时间（与订单一致）',
    current_attempt_id     BIGINT       NULL COMMENT '当前有效尝试 ID',
    successful_attempt_id  BIGINT       NULL COMMENT '最终成功尝试 ID',
    processing_started_at  DATETIME(3)  NULL COMMENT '开始处理时间',
    succeeded_at           DATETIME(3)  NULL COMMENT '确认成功时间',
    failed_at              DATETIME(3)  NULL COMMENT '确定失败时间',
    closed_at              DATETIME(3)  NULL COMMENT '支付关闭时间',
    failure_code           VARCHAR(64)  NULL COMMENT '最终失败码',
    version                BIGINT       NOT NULL DEFAULT 0 COMMENT '并发控制版本',
    created_at             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_payment_order (order_id),
    KEY idx_payment_user (user_id, created_at, id),
    KEY idx_payment_status_expire (status, expire_at, id),
    CONSTRAINT chk_payment_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_payment_status CHECK (status IN ('CREATED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CLOSED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付业务单';

-- 2) 支付执行尝试（一次支付业务可多次尝试，同一时刻最多一个非终态尝试）
CREATE TABLE ft_payment_attempt (
    id                  BIGINT       NOT NULL COMMENT '雪花主键',
    payment_id          BIGINT       NOT NULL COMMENT '所属支付业务 ID',
    attempt_no          INT          NOT NULL COMMENT '第几次尝试',
    attempt_request_id  VARCHAR(64)  NOT NULL COMMENT '客户端支付幂等请求号（唯一）',
    channel_request_no  VARCHAR(64)  NOT NULL COMMENT '模拟渠道执行编号（唯一）',
    status              VARCHAR(24)  NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PROCESSING/SUCCEEDED/FAILED/CLOSED/UNKNOWN',
    amount_cent         BIGINT       NOT NULL COMMENT '本次尝试金额',
    result_code         VARCHAR(64)  NULL COMMENT '模拟渠道结果码',
    result_message      VARCHAR(255) NULL COMMENT '结果描述（脱敏）',
    started_at          DATETIME(3)  NOT NULL COMMENT '开始执行时间',
    finished_at         DATETIME(3)  NULL COMMENT '完成时间',
    created_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attempt_no (payment_id, attempt_no),
    UNIQUE KEY uk_attempt_request (attempt_request_id),
    UNIQUE KEY uk_attempt_channel_no (channel_request_no),
    KEY idx_attempt_payment (payment_id, created_at, id),
    CONSTRAINT chk_attempt_status CHECK (status IN ('CREATED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CLOSED', 'UNKNOWN'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付执行尝试';

-- 3) 模拟支付回调（(channel, channel_event_id) 唯一去重，防重复通知重复确认）
CREATE TABLE ft_payment_callback (
    id               BIGINT       NOT NULL COMMENT '雪花主键',
    payment_id       BIGINT       NOT NULL COMMENT '所属支付业务 ID',
    attempt_id       BIGINT       NULL COMMENT '关联尝试 ID',
    channel          VARCHAR(24)  NOT NULL COMMENT '模拟渠道',
    channel_event_id VARCHAR(64)  NOT NULL COMMENT '渠道事件唯一编号',
    status           VARCHAR(24)  NOT NULL DEFAULT 'RECEIVED' COMMENT 'RECEIVED/PROCESSED',
    result_code      VARCHAR(64)  NULL COMMENT '渠道结果码',
    received_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '接收时间（UTC）',
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_callback_event (channel, channel_event_id),
    KEY idx_callback_payment (payment_id, received_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付回调记录';

-- 4) 票务权益（一票一权益，支付成功时为购买数量生成对应数量的有效凭证）
CREATE TABLE ft_ticket_entitlement (
    id               BIGINT       NOT NULL COMMENT '雪花主键',
    entitlement_no   VARCHAR(40)  NOT NULL COMMENT '票务权益编号（凭证号，唯一）',
    order_id         BIGINT       NOT NULL COMMENT '所属订单 ID',
    order_item_id    BIGINT       NOT NULL COMMENT '所属订单明细 ID',
    user_id          BIGINT       NOT NULL COMMENT '持有人用户 ID',
    event_id         BIGINT       NOT NULL COMMENT '活动 ID',
    event_title      VARCHAR(128) NOT NULL COMMENT '活动名称（成交快照）',
    session_id       BIGINT       NOT NULL COMMENT '场次 ID',
    session_start_at DATETIME(3)  NOT NULL COMMENT '演出开始时间（成交快照）',
    venue_name       VARCHAR(128) NOT NULL COMMENT '场馆名称（成交快照）',
    tier_id          BIGINT       NOT NULL COMMENT '票档 ID',
    tier_name        VARCHAR(64)  NOT NULL COMMENT '票档名称（成交快照）',
    ticket_index     INT          NOT NULL COMMENT '本明细内第几张票（从 1 起）',
    status           VARCHAR(24)  NOT NULL DEFAULT 'VALID' COMMENT 'VALID/USED/REVOKED',
    version          BIGINT       NOT NULL DEFAULT 0 COMMENT '并发控制版本',
    issued_at        DATETIME(3)  NOT NULL COMMENT '出票时间（UTC）',
    used_at          DATETIME(3)  NULL COMMENT '核销使用时间',
    revoked_at       DATETIME(3)  NULL COMMENT '撤销时间（退款）',
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entitlement_no (entitlement_no),
    UNIQUE KEY uk_entitlement_item_idx (order_item_id, ticket_index),
    KEY idx_entitlement_user (user_id, created_at, id),
    KEY idx_entitlement_order (order_id),
    CONSTRAINT chk_entitlement_status CHECK (status IN ('VALID', 'USED', 'REVOKED')),
    CONSTRAINT chk_entitlement_idx CHECK (ticket_index > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='票务权益';

-- 5) 支付域权限点种子（id 229，承接 M08 order:read 228）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (229, 'payment:read', '支付查询', 'API', 'ACTIVE');

-- 6) 角色-权限关系种子（FINANCE 管财务对账，SUPER_ADMIN 全量）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- FINANCE (role_id = 3)
    (734, 3, 229),
    -- SUPER_ADMIN (role_id = 5)
    (735, 5, 229);

-- 7) 扩展订单操作流水类型：M10 新增 PAYMENT_CONFIRMED（支付确认）
--    V9 的 CHECK 约束未包含该枚举，需增量放宽（订单操作流水支持支付确认审计）。
ALTER TABLE ft_order_operation DROP CHECK chk_op_type;
ALTER TABLE ft_order_operation
    ADD CONSTRAINT chk_op_type CHECK (operation IN ('ORDER_CREATED', 'USER_CANCELLED', 'TIMEOUT_CLOSED', 'PAYMENT_CONFIRMED'));
