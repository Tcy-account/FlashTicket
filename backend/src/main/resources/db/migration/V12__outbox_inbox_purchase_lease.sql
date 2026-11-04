-- =====================================================================
-- FlashTicket M12 · RocketMQ 异步抢票与可靠消息
-- 变更：扩展 ft_purchase_request（处理租约/恢复字段）；新建 ft_outbox_event /
--       ft_message_inbox（可靠消息基础设施）。
-- 依据：doc/抢票请求与异步排队模块.md（四 状态机；五 租约；十一 实体规划）
--       doc/全局数据库模型与ER关系设计.md（十四 共享基础设施域）
--       doc/全局逻辑ER模型.md（05 抢票请求与异步排队域）
-- 说明：抢票由 M08 同步建单升级为「可靠受理(QUEUED) + Outbox 同事务 → RocketMQ
--       → 消费者领取租约 → 建单(SUCCEEDED/FAILED)」。租约/恢复字段支撑消费者
--       崩溃后的受控恢复，Inbox 支撑消息重复投递的消费幂等。
-- =====================================================================

-- 1) 扩展 ft_purchase_request：异步处理租约与异常恢复字段
ALTER TABLE ft_purchase_request
    ADD COLUMN event_id             BIGINT       NULL COMMENT '活动 ID（受理时服务端据 tier 确认，追踪用）',
    ADD COLUMN session_id           BIGINT       NULL COMMENT '场次 ID（受理时服务端确认）',
    ADD COLUMN attempt_count        INT          NOT NULL DEFAULT 0 COMMENT '已开始的处理尝试次数',
    ADD COLUMN lease_owner          VARCHAR(64)  NULL COMMENT '当前处理者唯一令牌（租约）',
    ADD COLUMN lease_until          DATETIME(3)  NULL COMMENT '处理租约到期时间（UTC）',
    ADD COLUMN next_retry_at        DATETIME(3)  NULL COMMENT '下次允许恢复/重试时间（UTC）',
    ADD COLUMN last_error_code      VARCHAR(64)  NULL COMMENT '最近一次技术故障码',
    ADD COLUMN processing_started_at DATETIME(3) NULL COMMENT '最近一次开始处理时间（UTC）',
    ADD COLUMN accepted_deadline_at DATETIME(3)  NULL COMMENT '排队允许开始处理的截止时间（UTC）',
    ADD COLUMN completed_at         DATETIME(3)  NULL COMMENT '进入终态时间（UTC）';

-- 2) 状态机扩展：QUEUED/PROCESSING/RECOVERING/SUCCEEDED/FAILED/CANCELLED
ALTER TABLE ft_purchase_request DROP CHECK chk_pr_status;
ALTER TABLE ft_purchase_request
    ADD CONSTRAINT chk_pr_status CHECK (status IN ('QUEUED', 'PROCESSING', 'RECOVERING', 'SUCCEEDED', 'FAILED', 'CANCELLED'));

-- 3) 恢复/租约扫描索引
ALTER TABLE ft_purchase_request
    ADD KEY idx_purchase_recovery (status, next_retry_at, id),
    ADD KEY idx_purchase_lease (status, lease_until, id);

-- 4) Outbox 可靠待发布事件表（与业务状态同事务写入，后台 Publisher 补发）
CREATE TABLE ft_outbox_event (
    id             BIGINT       NOT NULL COMMENT '雪花主键',
    event_id       VARCHAR(64)  NOT NULL COMMENT '事件唯一标识（幂等键）',
    aggregate_type VARCHAR(32)  NOT NULL COMMENT '归属聚合类型（PURCHASE_REQUEST 等）',
    aggregate_id   VARCHAR(64)  NOT NULL COMMENT '归属聚合标识（request_id）',
    topic          VARCHAR(64)  NOT NULL COMMENT '目标 RocketMQ topic',
    event_type     VARCHAR(64)  NOT NULL COMMENT '事件类型（PURCHASE_SUBMITTED）',
    payload        TEXT         NOT NULL COMMENT '消息载荷（JSON 字符串）',
    schema_version INT          NOT NULL DEFAULT 1 COMMENT '载荷 schema 版本',
    status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
    attempt_count  INT          NOT NULL DEFAULT 0 COMMENT '已尝试发送次数',
    next_retry_at  DATETIME(3)  NULL COMMENT '下次重试时间（UTC）',
    lease_owner    VARCHAR(64)  NULL COMMENT '发布器领取租约令牌',
    lease_until    DATETIME(3)  NULL COMMENT '发布租约到期时间（UTC）',
    last_error     VARCHAR(500) NULL COMMENT '最近发送错误摘要',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    sent_at        DATETIME(3)  NULL COMMENT '发送成功时间（UTC）',
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event (event_id),
    KEY idx_outbox_status_retry (status, next_retry_at, id),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Outbox 可靠待发布事件';

-- 5) Inbox 消费幂等表（与消费端业务写入同事务提交）
CREATE TABLE ft_message_inbox (
    id            BIGINT       NOT NULL COMMENT '雪花主键',
    consumer_name VARCHAR(64)  NOT NULL COMMENT '消费者名（消费组标识）',
    event_id      VARCHAR(64)  NOT NULL COMMENT '已消费消息事件 ID',
    processed_at  DATETIME(3)  NOT NULL COMMENT '消费完成时间（UTC）',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_inbox (consumer_name, event_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='消息消费幂等（Inbox）';

-- 6) 抢票请求域权限点种子（id 233 起，承接 M11 refund:retry 232）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (233, 'purchase:read', '抢票请求查询', 'API', 'ACTIVE'),
    (234, 'purchase:retry', '抢票请求重试', 'API', 'ACTIVE');

-- 7) 角色-权限关系种子（OPERATOR/FINANCE 查，SUPER_ADMIN 查+重试）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- OPERATOR (role_id = 2)
    (742, 2, 233),
    -- FINANCE (role_id = 3)
    (743, 3, 233),
    -- SUPER_ADMIN (role_id = 5)
    (744, 5, 233),
    (745, 5, 234);
