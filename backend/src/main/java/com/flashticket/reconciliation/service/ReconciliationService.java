package com.flashticket.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.reconciliation.dto.ReconcileIssue;
import com.flashticket.reconciliation.entity.ReconcileException;
import com.flashticket.reconciliation.entity.ReconcileJob;
import com.flashticket.reconciliation.entity.ReconcileReport;
import com.flashticket.reconciliation.enums.ReconcileExceptionStatus;
import com.flashticket.reconciliation.enums.ReconcileJobStatus;
import com.flashticket.reconciliation.enums.ReconcileScope;
import com.flashticket.reconciliation.enums.ReconcileSeverity;
import com.flashticket.reconciliation.mapper.ReconcileExceptionMapper;
import com.flashticket.reconciliation.mapper.ReconcileJobMapper;
import com.flashticket.reconciliation.mapper.ReconcileReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 系统对账引擎（M16-B）：只读校验四领域账本，差异登记为工单。
 *
 * <p>三条硬约束（见设计文档第五节）：
 * 1. 只读，不改动交易账本；
 * 2. 幂等，同实体未关闭工单不重复生成；
 * 3. 资金/库存差异默认不自动修复，仅登记。
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ReconcileJobMapper jobMapper;
    private final ReconcileExceptionMapper exceptionMapper;
    private final ReconcileReportMapper reportMapper;
    private final ObjectMapper objectMapper;

    public ReconciliationService(JdbcTemplate jdbcTemplate,
                                 ReconcileJobMapper jobMapper,
                                 ReconcileExceptionMapper exceptionMapper,
                                 ReconcileReportMapper reportMapper,
                                 ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobMapper = jobMapper;
        this.exceptionMapper = exceptionMapper;
        this.reportMapper = reportMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行一次对账：创建任务 → 校验 → 差异落工单（幂等）→ 更新任务 → 写汇总。
     */
    @Transactional
    public ReconcileJob runReconcile(ReconcileScope scope, Long triggeredBy) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ReconcileJob job = new ReconcileJob();
        job.setJobNo(nextNo("JOB"));
        job.setScope(scope.name());
        job.setStatus(ReconcileJobStatus.RUNNING.name());
        job.setTriggeredBy(triggeredBy);
        job.setStartedAt(now);
        jobMapper.insert(job);

        try {
            List<ReconcileIssue> issues = check(scope);
            int checked = countChecked(scope);
            int inserted = persistIssues(job.getId(), scope, issues);

            job.setStatus(ReconcileJobStatus.SUCCEEDED.name());
            job.setTotalChecked(checked);
            job.setExceptionCount(inserted);
            job.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
            jobMapper.updateById(job);

            ReconcileReport report = new ReconcileReport();
            report.setJobId(job.getId());
            report.setScope(scope.name());
            report.setTotalChecked(checked);
            report.setExceptionCount(inserted);
            report.setSummary(toSummary(scope, issues));
            reportMapper.insert(report);
            return job;
        } catch (Exception e) {
            log.error("对账任务失败 scope={}", scope, e);
            job.setStatus(ReconcileJobStatus.FAILED.name());
            job.setErrorMsg(truncate(e.getMessage()));
            job.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
            jobMapper.updateById(job);
            throw e;
        }
    }

    /** 差异落工单，同实体未关闭工单幂等跳过。 */
    private int persistIssues(Long jobId, ReconcileScope scope, List<ReconcileIssue> issues) {
        int inserted = 0;
        for (ReconcileIssue issue : issues) {
            int open = exceptionMapper.countOpenByEntity(
                    scope.name(), issue.checkType(), issue.entityType(), issue.entityId());
            if (open > 0) {
                continue;
            }
            ReconcileException ex = new ReconcileException();
            ex.setExceptionNo(nextNo("REC"));
            ex.setJobId(jobId);
            ex.setScope(scope.name());
            ex.setCheckType(issue.checkType());
            ex.setSeverity(issue.severity().name());
            ex.setStatus(ReconcileExceptionStatus.OPEN.name());
            ex.setEntityType(issue.entityType());
            ex.setEntityId(issue.entityId());
            ex.setDescription(issue.description());
            ex.setEvidence(issue.evidence());
            exceptionMapper.insert(ex);
            inserted++;
        }
        return inserted;
    }

    /** 分派到具体领域的校验。 */
    private List<ReconcileIssue> check(ReconcileScope scope) {
        return switch (scope) {
            case INVENTORY -> checkInventory();
            case ORDER -> checkOrder();
            case PAYMENT -> checkPayment();
            case REFUND -> checkRefund();
        };
    }

    /* ---------- 库存对账：总量守恒 + 非负 ---------- */

    private List<ReconcileIssue> checkInventory() {
        List<ReconcileIssue> issues = new ArrayList<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT tier_id, total, available, reserved, sold FROM ft_inventory "
                        + "WHERE total != available + reserved + sold")) {
            String tierId = String.valueOf(row.get("tier_id"));
            String evidence = toJson(row);
            issues.add(new ReconcileIssue("INVENTORY_CONSERVATION", ReconcileSeverity.CRITICAL,
                    "INVENTORY", tierId,
                    "库存总量守恒破坏 total=" + row.get("total") + " available=" + row.get("available")
                            + " reserved=" + row.get("reserved") + " sold=" + row.get("sold"),
                    evidence));
        }
        for (Long tierId : jdbcTemplate.queryForList(
                "SELECT tier_id FROM ft_inventory WHERE available < 0 OR reserved < 0 OR sold < 0",
                Long.class)) {
            issues.add(new ReconcileIssue("INVENTORY_NEGATIVE", ReconcileSeverity.CRITICAL,
                    "INVENTORY", String.valueOf(tierId), "库存出现负值", null));
        }
        return issues;
    }

    /* ---------- 订单对账：过期待支付未关单 ---------- */

    private List<ReconcileIssue> checkOrder() {
        List<ReconcileIssue> issues = new ArrayList<>();
        List<Long> expired = jdbcTemplate.queryForList(
                "SELECT id FROM ft_order WHERE status = 'PENDING_PAYMENT' AND expire_at <= ?",
                Long.class, LocalDateTime.now(ZoneOffset.UTC));
        for (Long orderId : expired) {
            issues.add(new ReconcileIssue("ORDER_EXPIRED_PENDING", ReconcileSeverity.MEDIUM,
                    "ORDER", String.valueOf(orderId), "待支付订单已过期但未关闭", null));
        }
        return issues;
    }

    /* ---------- 支付对账：PAID 订单支付一致性 ---------- */

    private List<ReconcileIssue> checkPayment() {
        List<ReconcileIssue> issues = new ArrayList<>();
        for (Long orderId : jdbcTemplate.queryForList(
                "SELECT o.id FROM ft_order o WHERE o.status = 'PAID' AND NOT EXISTS "
                        + "(SELECT 1 FROM ft_payment p WHERE p.order_id = o.id AND p.status = 'SUCCEEDED')",
                Long.class)) {
            issues.add(new ReconcileIssue("PAYMENT_MISSING", ReconcileSeverity.CRITICAL,
                    "ORDER", String.valueOf(orderId), "已支付订单缺少成功支付记录", null));
        }
        for (Long orderId : jdbcTemplate.queryForList(
                "SELECT order_id FROM ft_payment WHERE status = 'SUCCEEDED' "
                        + "GROUP BY order_id HAVING COUNT(*) > 1", Long.class)) {
            issues.add(new ReconcileIssue("PAYMENT_DUPLICATE", ReconcileSeverity.CRITICAL,
                    "ORDER", String.valueOf(orderId), "订单存在多条成功支付记录", null));
        }
        for (Long orderId : jdbcTemplate.queryForList(
                "SELECT p.order_id FROM ft_payment p JOIN ft_order o ON o.id = p.order_id "
                        + "WHERE p.status = 'SUCCEEDED' AND o.status = 'PAID' "
                        + "AND p.amount_cent != o.paid_amount_cent", Long.class)) {
            issues.add(new ReconcileIssue("PAYMENT_AMOUNT_MISMATCH", ReconcileSeverity.CRITICAL,
                    "ORDER", String.valueOf(orderId), "支付金额与订单应付金额不一致", null));
        }
        return issues;
    }

    /* ---------- 退款对账：重复退款 + 超额退款 ---------- */

    private List<ReconcileIssue> checkRefund() {
        List<ReconcileIssue> issues = new ArrayList<>();
        for (Long orderId : jdbcTemplate.queryForList(
                "SELECT order_id FROM ft_refund WHERE status = 'SUCCEEDED' "
                        + "GROUP BY order_id HAVING COUNT(*) > 1", Long.class)) {
            issues.add(new ReconcileIssue("REFUND_DUPLICATE", ReconcileSeverity.CRITICAL,
                    "ORDER", String.valueOf(orderId), "订单存在多条成功退款记录", null));
        }
        for (Long orderId : jdbcTemplate.queryForList(
                "SELECT r.order_id FROM ft_refund r "
                        + "JOIN ft_payment p ON p.order_id = r.order_id AND p.status = 'SUCCEEDED' "
                        + "WHERE r.status = 'SUCCEEDED' "
                        + "GROUP BY r.order_id, p.amount_cent "
                        + "HAVING SUM(r.amount_cent) > p.amount_cent", Long.class)) {
            issues.add(new ReconcileIssue("REFUND_OVER_PAY", ReconcileSeverity.CRITICAL,
                    "ORDER", String.valueOf(orderId), "累计退款金额超过原支付金额", null));
        }
        return issues;
    }

    /* ---------- 辅助 ---------- */

    private int countChecked(ReconcileScope scope) {
        String table = switch (scope) {
            case INVENTORY -> "ft_inventory";
            case ORDER -> "ft_order";
            case PAYMENT -> "ft_payment";
            case REFUND -> "ft_refund";
        };
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return n == null ? 0 : n;
    }

    private String toJson(Map<String, Object> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String toSummary(ReconcileScope scope, List<ReconcileIssue> issues) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("scope", scope.name(), "issueCount", issues.size()));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String nextNo(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-"
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
