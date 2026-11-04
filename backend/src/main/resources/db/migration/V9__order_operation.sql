-- =====================================================================
-- FlashTicket M09 · 订单取消与支付超时关单
-- 建表：ft_order_operation（订单操作流水/状态变更历史）
-- 依据：doc/订单与交易状态机模块.md（九、数据模型；八、操作日志）
--        doc/全局数据库模型与ER关系设计.md（八、订单与交易域）
-- 约定：ft_ 前缀、雪花 BIGINT 主键、DATETIME(3) UTC、枚举大写业务码
-- 说明：M09 落地订单状态机 PENDING_PAYMENT → CLOSED（用户取消 + 支付超时），
--       关单与库存释放/限购恢复在同一本地事务完成（条件更新争夺状态转换权，
--       重复取消/重复调度幂等）。操作流水用于业务审计，operation_id 唯一防重。
-- =====================================================================

-- 订单操作流水（订单状态变更历史，用于业务审计与故障恢复）
CREATE TABLE ft_order_operation (
    id           BIGINT       NOT NULL COMMENT '雪花主键',
    order_id     BIGINT       NOT NULL COMMENT '所属订单 ID',
    operation    VARCHAR(24)  NOT NULL COMMENT '操作类型 ORDER_CREATED/USER_CANCELLED/TIMEOUT_CLOSED',
    from_status  VARCHAR(24)  NULL COMMENT '操作前状态（创建时为 NULL）',
    to_status    VARCHAR(24)  NOT NULL COMMENT '操作后状态 PENDING_PAYMENT/PAID/CLOSED',
    actor_id     BIGINT       NULL COMMENT '操作主体（用户取消=用户 ID，系统关单=NULL）',
    reason       VARCHAR(32)  NULL COMMENT '关单原因 USER_CANCEL/PAYMENT_TIMEOUT',
    operation_id VARCHAR(64)  NOT NULL COMMENT '业务操作 ID（幂等键，唯一）',
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_operation (operation_id),
    KEY idx_order_op_order (order_id, id),
    CONSTRAINT chk_op_type CHECK (operation IN ('ORDER_CREATED', 'USER_CANCELLED', 'TIMEOUT_CLOSED')),
    CONSTRAINT chk_op_to_status CHECK (to_status IN ('PENDING_PAYMENT', 'PAID', 'CLOSED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='订单操作流水';
