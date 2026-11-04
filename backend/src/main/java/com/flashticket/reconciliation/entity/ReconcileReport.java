package com.flashticket.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 对账结果汇总（表 ft_reconcile_report，每次任务一份）。
 */
@TableName("ft_reconcile_report")
public class ReconcileReport {

    private Long id;

    private Long jobId;

    private String scope;

    private Integer totalChecked;

    private Integer exceptionCount;

    private String summary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getTotalChecked() {
        return totalChecked;
    }

    public void setTotalChecked(Integer totalChecked) {
        this.totalChecked = totalChecked;
    }

    public Integer getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(Integer exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
