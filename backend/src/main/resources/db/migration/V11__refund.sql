-- =====================================================================
-- FlashTicket M11 · 全额退款与权益撤销
-- 建表：ft_refund / ft_refund_attempt
-- 依据：doc/模拟支付与退款模块.md（六、退款业务需求；七、退款状态机；八、数据模型）
--        doc/全局逻辑ER模型.md（六、ft_refund；七、ft_refund_attempt）
--        doc/全局数据库模型与ER关系设计.md（九、模拟支付与退款域）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码、金额 BIGINT 分
-- 说明：退款是一笔新的逆向交易，不删除原支付记录、不把订单改回待支付。V1 仅
--       全额退款（refund.amount_cent = payment.amount_cent），同一订单最多一笔退款
--       业务（UNIQUE(order_id)）。退款成功后：订单保持 PAID、库存 sold 不变、优惠券
--       保持 USED，仅撤销票务权益（REVOKED），不自动返还库存与优惠券。
-- =====================================================================

-- 1) 退款业务单（V1 一订单一退款业务，全额退款）
CREATE TABLE ft_refund (
    id                 BIGINT       NOT NULL COMMENT '雪花主键',
    refund_no          VARCHAR(40)  NOT NULL COMMENT '退款业务号（唯一）',
    refund_request_id  VARCHAR(64)  NOT NULL COMMENT '退款申请幂等号（唯一）',
    payment_id         BIGINT       NOT NULL COMMENT '原支付单 ID',
    order_id           BIGINT       NOT NULL COMMENT '原订单 ID（V1 一订单一退款业务）',
    user_id            BIGINT       NOT NULL COMMENT '退款申请人 ID',
    amount_cent        BIGINT       NOT NULL COMMENT '退款金额（分，全额 = 原支付金额）',
    currency           CHAR(3)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    reason_code        VARCHAR(32)  NOT NULL COMMENT '退款原因 PERSONAL/EVENT_POSTPONED/EVENT_CANCELLED/OTHER',
    reason_detail      VARCHAR(500) NULL COMMENT '补充说明',
    status             VARCHAR(24)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '退款状态',
    version            BIGINT       NOT NULL DEFAULT 0 COMMENT '并发控制版本',
    approved_by        BIGINT       NULL COMMENT '审核人 ID',
    approved_at        DATETIME(3)  NULL COMMENT '审核通过时间',
    rejected_by        BIGINT       NULL COMMENT '拒绝人 ID',
    rejected_at        DATETIME(3)  NULL COMMENT '拒绝时间',
    review_comment     VARCHAR(500) NULL COMMENT '审核意见',
    current_attempt_id BIGINT       NULL COMMENT '当前退款执行尝试 ID',
    succeeded_at       DATETIME(3)  NULL COMMENT '退款完成时间',
    failed_at          DATETIME(3)  NULL COMMENT '退款最终失败时间',
    created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_refund_request (refund_request_id),
    UNIQUE KEY uk_refund_order (order_id),
    KEY idx_refund_user (user_id, created_at, id),
    KEY idx_refund_status (status, created_at, id),
    KEY idx_refund_payment (payment_id, id),
    CONSTRAINT chk_refund_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_refund_status CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='退款业务单';

-- 2) 退款执行尝试（同一退款业务允许多次尝试，上次确定失败才允许新建下一次）
CREATE TABLE ft_refund_attempt (
    id                 BIGINT       NOT NULL COMMENT '雪花主键',
    refund_id          BIGINT       NOT NULL COMMENT '退款业务 ID',
    attempt_no         INT          NOT NULL COMMENT '第几次尝试',
    channel_request_no VARCHAR(64)  NOT NULL COMMENT '模拟退款渠道执行编号（唯一）',
    status             VARCHAR(24)  NOT NULL DEFAULT 'CREATED' COMMENT '执行状态',
    amount_cent        BIGINT       NOT NULL COMMENT '本次执行金额',
    result_code        VARCHAR(64)  NULL COMMENT '执行结果码',
    result_message     VARCHAR(255) NULL COMMENT '脱敏结果描述',
    started_at         DATETIME(3)  NOT NULL COMMENT '开始时间',
    finished_at        DATETIME(3)  NULL COMMENT '完成时间',
    created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_attempt_no (refund_id, attempt_no),
    UNIQUE KEY uk_refund_attempt_channel (channel_request_no),
    KEY idx_refund_attempt_refund (refund_id, created_at, id),
    KEY idx_refund_attempt_status (status, updated_at, id),
    CONSTRAINT chk_refund_attempt_status CHECK (status IN ('CREATED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'UNKNOWN', 'CLOSED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='退款执行尝试';

-- 3) 退款域权限点种子（id 230 起，承接 M10 payment:read 229）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (230, 'refund:read', '退款查询', 'API', 'ACTIVE'),
    (231, 'refund:approve', '退款审核', 'API', 'ACTIVE'),
    (232, 'refund:retry', '退款重试', 'API', 'ACTIVE');

-- 4) 角色-权限关系种子（FINANCE 管退款审核，SUPER_ADMIN 全量）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- FINANCE (role_id = 3)
    (736, 3, 230),
    (737, 3, 231),
    (738, 3, 232),
    -- SUPER_ADMIN (role_id = 5)
    (739, 5, 230),
    (740, 5, 231),
    (741, 5, 232);

-- 5) 扩展订单操作流水类型：M11 新增 REFUND_REQUESTED（申请退款）/ REFUND_SUCCEEDED（退款成功）
--    订单主状态均保持 PAID，退款详情的权威状态以 ft_refund 为准。
ALTER TABLE ft_order_operation DROP CHECK chk_op_type;
ALTER TABLE ft_order_operation
    ADD CONSTRAINT chk_op_type CHECK (operation IN ('ORDER_CREATED', 'USER_CANCELLED', 'TIMEOUT_CLOSED', 'PAYMENT_CONFIRMED', 'REFUND_REQUESTED', 'REFUND_SUCCEEDED'));
