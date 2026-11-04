package com.flashticket.reconciliation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.order.service.OrderCloseService;
import com.flashticket.reconciliation.entity.CompensationTask;
import com.flashticket.reconciliation.enums.CompensationStatus;
import com.flashticket.reconciliation.mapper.CompensationTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 补偿任务服务（M16-D）：登记 + 安全执行可确认正确的补偿动作。
 *
 * <p>原则（见设计文档第九节）：只执行能确认正确性且具备幂等保障的操作；
 * 资金/库存疑难差异进入人工复核（MANUAL_REVIEW），不自动猜测。
 * 执行器复用既有领域服务（如 {@link OrderCloseService}），补偿动作带 operationId 幂等。
 */
@Service
public class CompensationService {

    private static final Logger log = LoggerFactory.getLogger(CompensationService.class);

    private final CompensationTaskMapper taskMapper;
    private final OrderCloseService orderCloseService;

    public CompensationService(CompensationTaskMapper taskMapper,
                               OrderCloseService orderCloseService) {
        this.taskMapper = taskMapper;
        this.orderCloseService = orderCloseService;
    }

    /**
     * 创建补偿任务（幂等：同 operationId 返回已存在任务，不重复创建）。
     */
    @Transactional
    public CompensationTask create(String taskType, String entityType, String entityId, Long exceptionId) {
        String operationId = "COMP-" + taskType + "-" + entityType + "-" + entityId;
        CompensationTask existing = taskMapper.findByOperationId(operationId);
        if (existing != null) {
            return existing;
        }
        CompensationTask task = new CompensationTask();
        task.setOperationId(operationId);
        task.setTaskType(taskType);
        task.setStatus(CompensationStatus.PENDING.name());
        task.setExceptionId(exceptionId);
        task.setEntityType(entityType);
        task.setEntityId(entityId);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        taskMapper.insert(task);
        return task;
    }

    /**
     * 执行补偿任务。执行器在自己的事务中提交（如关单），本方法负责补偿状态流转，
     * 刻意不加事务，避免「捕获业务异常但事务已 rollback-only」的坑。
     */
    public CompensationTask execute(Long taskId) {
        CompensationTask task = requireTask(taskId);
        CompensationStatus status = CompensationStatus.valueOf(task.getStatus());
        if (status != CompensationStatus.PENDING && status != CompensationStatus.RETRY_WAIT) {
            throw new BizException(ErrorCode.COMPENSATION_ILLEGAL_TRANSITION);
        }
        task.setAttemptCount(task.getAttemptCount() + 1);
        try {
            dispatch(task);
            task.setStatus(CompensationStatus.SUCCEEDED.name());
            task.setLastErrorCode(null);
            task.setLastError(null);
            task.setNextRetryAt(null);
        } catch (BizException e) {
            // 确定性业务失败（如订单已被支付、不存在）：不重试，转人工复核
            task.setStatus(CompensationStatus.MANUAL_REVIEW.name());
            task.setLastErrorCode(e.getErrorCode().getCode());
            task.setLastError(truncate(e.getMessage()));
        } catch (Exception e) {
            log.error("补偿执行失败 taskId={}", taskId, e);
            // 技术性失败：未超限则退避重试，否则转人工复核
            if (task.getAttemptCount() >= task.getMaxAttempts()) {
                task.setStatus(CompensationStatus.MANUAL_REVIEW.name());
            } else {
                task.setStatus(CompensationStatus.RETRY_WAIT.name());
                task.setNextRetryAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(30));
            }
            task.setLastError(truncate(e.getMessage()));
        }
        taskMapper.updateById(task);
        return task;
    }

    /** 补偿任务分页。 */
    public PageResult<CompensationTask> list(long page, long size) {
        Page<CompensationTask> p = new Page<>(page, size);
        IPage<CompensationTask> r = taskMapper.selectPage(p,
                new LambdaQueryWrapper<CompensationTask>().orderByDesc(CompensationTask::getId));
        return new PageResult<>(r.getRecords(), r.getTotal(), r.getCurrent(), r.getSize());
    }

    public CompensationTask get(Long id) {
        return requireTask(id);
    }

    /* ---------- 内部 ---------- */

    private void dispatch(CompensationTask task) {
        switch (task.getTaskType()) {
            case "CLOSE_ORDER" -> orderCloseService.closeByTimeout(Long.valueOf(task.getEntityId()));
            default -> throw new BizException(ErrorCode.COMPENSATION_ILLEGAL_TRANSITION);
        }
    }

    private CompensationTask requireTask(Long id) {
        CompensationTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException(ErrorCode.COMPENSATION_NOT_FOUND);
        }
        return task;
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
