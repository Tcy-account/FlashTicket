-- M16-B 系统对账与异常补偿：业务表（5 张）
-- 对账只读校验，不改动交易账本；差异登记为工单，补偿动作幂等。

-- 1) 对账任务
CREATE TABLE ft_reconcile_job (
    id              BIGINT       PRIMARY KEY,
    job_no          VARCHAR(64)  NOT NULL,
    scope           VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    total_checked   INT          NOT NULL DEFAULT 0,
    exception_count INT          NOT NULL DEFAULT 0,
    triggered_by    BIGINT       NULL,
    started_at      DATETIME(3)  NULL,
    completed_at    DATETIME(3)  NULL,
    error_msg       VARCHAR(512) NULL,
    created_at      DATETIME(3)  NOT NULL,
    updated_at      DATETIME(3)  NOT NULL,
    UNIQUE KEY uk_reconcile_job_no (job_no),
    KEY idx_reconcile_job_status (status)
);

-- 2) 异常工单
CREATE TABLE ft_reconcile_exception (
    id           BIGINT       PRIMARY KEY,
    exception_no VARCHAR(64)  NOT NULL,
    job_id       BIGINT       NOT NULL,
    scope        VARCHAR(32)  NOT NULL,
    check_type   VARCHAR(64)  NOT NULL,
    severity     VARCHAR(16)  NOT NULL,
    status       VARCHAR(32)  NOT NULL,
    entity_type  VARCHAR(32)  NOT NULL,
    entity_id    VARCHAR(64)  NOT NULL,
    description  VARCHAR(512) NOT NULL,
    evidence     TEXT         NULL,
    resolution   VARCHAR(32)  NULL,
    resolved_by  BIGINT       NULL,
    resolved_at  DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL,
    updated_at   DATETIME(3)  NOT NULL,
    UNIQUE KEY uk_reconcile_exception_no (exception_no),
    KEY idx_reconcile_exception_job (job_id),
    KEY idx_reconcile_exception_status (status),
    KEY idx_reconcile_exception_entity (scope, check_type, entity_type, entity_id)
);

-- 3) 异常处理记录（审计）
CREATE TABLE ft_reconcile_action (
    id           BIGINT       PRIMARY KEY,
    exception_id BIGINT       NOT NULL,
    action_type  VARCHAR(32)  NOT NULL,
    operator_id  BIGINT       NOT NULL,
    comment      VARCHAR(512) NULL,
    created_at   DATETIME(3)  NOT NULL,
    KEY idx_reconcile_action_exception (exception_id)
);

-- 4) 补偿任务（幂等 + 租约 + 重试上限）
CREATE TABLE ft_compensation_task (
    id              BIGINT       PRIMARY KEY,
    operation_id    VARCHAR(64)  NOT NULL,
    task_type       VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    exception_id    BIGINT       NULL,
    entity_type     VARCHAR(32)  NOT NULL,
    entity_id       VARCHAR(64)  NOT NULL,
    attempt_count   INT          NOT NULL DEFAULT 0,
    max_attempts    INT          NOT NULL DEFAULT 3,
    next_retry_at   DATETIME(3)  NULL,
    last_error_code VARCHAR(64)  NULL,
    last_error      VARCHAR(512) NULL,
    lease_owner     VARCHAR(64)  NULL,
    lease_until     DATETIME(3)  NULL,
    created_at      DATETIME(3)  NOT NULL,
    updated_at      DATETIME(3)  NOT NULL,
    UNIQUE KEY uk_compensation_operation (operation_id),
    KEY idx_compensation_status (status),
    KEY idx_compensation_exception (exception_id)
);

-- 5) 对账结果汇总（每次任务一份）
CREATE TABLE ft_reconcile_report (
    id              BIGINT      PRIMARY KEY,
    job_id          BIGINT      NOT NULL,
    scope           VARCHAR(32) NOT NULL,
    total_checked   INT         NOT NULL DEFAULT 0,
    exception_count INT         NOT NULL DEFAULT 0,
    summary         TEXT        NULL,
    created_at      DATETIME(3) NOT NULL,
    UNIQUE KEY uk_reconcile_report_job (job_id)
);
