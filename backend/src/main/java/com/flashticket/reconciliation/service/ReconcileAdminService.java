package com.flashticket.reconciliation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.reconciliation.entity.ReconcileAction;
import com.flashticket.reconciliation.entity.ReconcileException;
import com.flashticket.reconciliation.entity.ReconcileJob;
import com.flashticket.reconciliation.enums.ReconcileExceptionStatus;
import com.flashticket.reconciliation.enums.ReconcileScope;
import com.flashticket.reconciliation.mapper.ReconcileActionMapper;
import com.flashticket.reconciliation.mapper.ReconcileExceptionMapper;
import com.flashticket.reconciliation.mapper.ReconcileJobMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 对账后台管理（M16-C）：任务/工单查询 + 异常复核（状态流转 + 审计）。
 *
 * <p>对账引擎 {@link ReconciliationService} 负责发现差异，本服务负责差异的管理闭环。
 */
@Service
public class ReconcileAdminService {

    private static final List<String> RESOLUTIONS = List.of("RESOLVED", "FALSE_POSITIVE");

    private final ReconcileJobMapper jobMapper;
    private final ReconcileExceptionMapper exceptionMapper;
    private final ReconcileActionMapper actionMapper;
    private final ReconciliationService reconciliationService;

    public ReconcileAdminService(ReconcileJobMapper jobMapper,
                                 ReconcileExceptionMapper exceptionMapper,
                                 ReconcileActionMapper actionMapper,
                                 ReconciliationService reconciliationService) {
        this.jobMapper = jobMapper;
        this.exceptionMapper = exceptionMapper;
        this.actionMapper = actionMapper;
        this.reconciliationService = reconciliationService;
    }

    /** 创建并立即执行一次对账任务。 */
    public ReconcileJob execute(ReconcileScope scope, Long operatorId) {
        return reconciliationService.runReconcile(scope, operatorId);
    }

    /** 对账任务分页（按创建倒序）。 */
    public PageResult<ReconcileJob> listJobs(long page, long size) {
        Page<ReconcileJob> p = new Page<>(page, size);
        IPage<ReconcileJob> r = jobMapper.selectPage(p,
                new LambdaQueryWrapper<ReconcileJob>().orderByDesc(ReconcileJob::getId));
        return new PageResult<>(r.getRecords(), r.getTotal(), r.getCurrent(), r.getSize());
    }

    public ReconcileJob getJob(Long id) {
        ReconcileJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException(ErrorCode.RECONCILE_JOB_NOT_FOUND);
        }
        return job;
    }

    /** 异常工单分页（按状态筛选，按创建倒序）。 */
    public PageResult<ReconcileException> listExceptions(String status, long page, long size) {
        Page<ReconcileException> p = new Page<>(page, size);
        LambdaQueryWrapper<ReconcileException> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(ReconcileException::getStatus, status);
        }
        qw.orderByDesc(ReconcileException::getId);
        IPage<ReconcileException> r = exceptionMapper.selectPage(p, qw);
        return new PageResult<>(r.getRecords(), r.getTotal(), r.getCurrent(), r.getSize());
    }

    public ReconcileException getException(Long id) {
        ReconcileException ex = exceptionMapper.selectById(id);
        if (ex == null) {
            throw new BizException(ErrorCode.RECONCILE_EXCEPTION_NOT_FOUND);
        }
        return ex;
    }

    /** 某工单的处理记录。 */
    public List<ReconcileAction> listActions(Long exceptionId) {
        return actionMapper.selectList(new LambdaQueryWrapper<ReconcileAction>()
                .eq(ReconcileAction::getExceptionId, exceptionId)
                .orderByAsc(ReconcileAction::getId));
    }

    /**
     * 提交复核结论：OPEN/INVESTIGATING → RESOLVED，记录 resolution 与审计。
     * resolution 取值 RESOLVED（已解决）或 FALSE_POSITIVE（误报）。
     */
    @Transactional
    public ReconcileException review(Long id, String resolution, String comment, Long operatorId) {
        if (resolution == null || !RESOLUTIONS.contains(resolution)) {
            throw new BizException(ErrorCode.RECONCILE_ILLEGAL_TRANSITION);
        }
        ReconcileException ex = getException(id);
        ReconcileExceptionStatus current = ReconcileExceptionStatus.valueOf(ex.getStatus());
        if (current != ReconcileExceptionStatus.OPEN && current != ReconcileExceptionStatus.INVESTIGATING) {
            throw new BizException(ErrorCode.RECONCILE_ILLEGAL_TRANSITION);
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ex.setStatus(ReconcileExceptionStatus.RESOLVED.name());
        ex.setResolution(resolution);
        ex.setResolvedBy(operatorId);
        ex.setResolvedAt(now);
        exceptionMapper.updateById(ex);

        ReconcileAction action = new ReconcileAction();
        action.setExceptionId(id);
        action.setActionType("REVIEW");
        action.setOperatorId(operatorId);
        action.setComment(resolution + (comment == null || comment.isBlank() ? "" : " | " + comment));
        actionMapper.insert(action);
        return ex;
    }
}
