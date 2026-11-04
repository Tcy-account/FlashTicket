-- V14：系统通知与消息中心（M14）
-- 设计依据：doc/系统通知与消息中心模块.md
-- 数据模型：通知内容（ft_notification）与接收记录（ft_notification_recipient）分离，
-- 独立保存每个用户的已读状态；为 P1 全平台公告预留「一份内容 + 多接收人」的扩展能力。
-- 幂等：RocketMQ 消费靠 ft_message_inbox (consumer_name, event_id) 去重，接收记录
-- UNIQUE(notification_id, user_id) 兜底并发重复插入。

-- 1) 通知内容（与业务关联，标题正文为业务发生时快照，不随订单/活动改名而变）
CREATE TABLE ft_notification (
    id         BIGINT        NOT NULL COMMENT '雪花主键',
    type       VARCHAR(24)   NOT NULL COMMENT 'TRANSACTION/EVENT/MARKETING/SYSTEM',
    title      VARCHAR(200)  NOT NULL COMMENT '通知标题（快照）',
    content    VARCHAR(1000) NOT NULL COMMENT '通知正文（快照）',
    biz_type   VARCHAR(24)   NULL COMMENT '跳转业务类型 ORDER/PAYMENT/REFUND/PURCHASE',
    biz_id     VARCHAR(64)   NULL COMMENT '跳转业务 ID（雪花字符串，防 JS 精度丢失）',
    event_id   VARCHAR(64)   NOT NULL COMMENT '来源业务事件 ID（与 Inbox 幂等对应）',
    created_at DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    KEY idx_notification_type (type, id),
    CONSTRAINT chk_notification_type CHECK (type IN ('TRANSACTION', 'EVENT', 'MARKETING', 'SYSTEM'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知内容';

-- 2) 通知接收记录（每人每通知一条，read_at 空=未读；hidden_at 为 P1 软删预留）
CREATE TABLE ft_notification_recipient (
    id              BIGINT      NOT NULL COMMENT '雪花主键',
    notification_id BIGINT      NOT NULL COMMENT '通知内容 ID',
    user_id         BIGINT      NOT NULL COMMENT '接收用户 ID',
    read_at         DATETIME(3) NULL COMMENT '首次已读时间（空=未读）',
    hidden_at       DATETIME(3) NULL COMMENT '用户隐藏时间（P1 软删）',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间（UTC）',
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_recipient (notification_id, user_id),
    KEY idx_recipient_user (user_id, id),
    KEY idx_recipient_unread (user_id, read_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知接收记录';

-- 3) 通知域权限点种子（id 240 起，承接 M13 coupon:reconcile 239）
INSERT INTO ft_permission (id, code, name, type, status) VALUES
    (240, 'notification:read', '通知查询', 'API', 'ACTIVE'),
    (241, 'notification:retry', '通知重试', 'API', 'ACTIVE'),
    (242, 'notification:manage', '通知模板管理', 'API', 'ACTIVE');

-- 4) 角色-权限关系（OPERATOR 查+重试，FINANCE 查，SUPER_ADMIN 全量）
INSERT INTO ft_role_permission (id, role_id, permission_id) VALUES
    -- notification:read (240) → OPERATOR(2), FINANCE(3), SUPER_ADMIN(5)
    (756, 2, 240), (757, 3, 240), (758, 5, 240),
    -- notification:retry (241) → OPERATOR(2), SUPER_ADMIN(5)
    (759, 2, 241), (760, 5, 241),
    -- notification:manage (242) → SUPER_ADMIN(5)
    (761, 5, 242);
