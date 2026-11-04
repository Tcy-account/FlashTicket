package com.flashticket.analytics.dto;

/**
 * 抢票链路转化统计（受理/成功/失败/处理中 + 成功率）。
 */
public class PurchaseConversionResponse {

    /** 抢票受理数（已持久化请求总数）。 */
    private Long acceptedCount;

    /** 抢票成功数（成功建单）。 */
    private Long succeededCount;

    /** 抢票失败数（确定性业务失败 + 排队超时）。 */
    private Long failedCount;

    /** 处理中数（QUEUED/PROCESSING/RECOVERING）。 */
    private Long processingCount;

    /** 抢票成功率 = 成功数 / (成功数 + 失败数)。 */
    private Double successRate;

    public Long getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(Long acceptedCount) {
        this.acceptedCount = acceptedCount;
    }

    public Long getSucceededCount() {
        return succeededCount;
    }

    public void setSucceededCount(Long succeededCount) {
        this.succeededCount = succeededCount;
    }

    public Long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Long failedCount) {
        this.failedCount = failedCount;
    }

    public Long getProcessingCount() {
        return processingCount;
    }

    public void setProcessingCount(Long processingCount) {
        this.processingCount = processingCount;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }
}
