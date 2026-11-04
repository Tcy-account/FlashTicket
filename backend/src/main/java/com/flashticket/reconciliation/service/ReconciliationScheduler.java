package com.flashticket.reconciliation.service;

import com.flashticket.reconciliation.enums.ReconcileScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时对账调度器（M16-C）：周期性扫描四个对账领域。
 *
 * <p>对账是只读防御性检查，重复执行幂等（同实体未关闭工单不重复生成）；
 * 单领域失败不影响其他领域。test profile 下关闭调度，避免集成测试期间异步干扰。
 */
@Component
@Profile("!test")
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    private final ReconciliationService reconciliationService;

    public ReconciliationScheduler(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /** 每隔固定延迟对四个领域各执行一次对账。 */
    @Scheduled(fixedDelayString = "${flashticket.reconciliation.scan-delay-ms:300000}",
            initialDelayString = "${flashticket.reconciliation.scan-delay-ms:300000}")
    public void reconcileAll() {
        for (ReconcileScope scope : ReconcileScope.values()) {
            try {
                reconciliationService.runReconcile(scope, null);
            } catch (Exception e) {
                log.error("定时对账失败 scope={}", scope, e);
            }
        }
    }
}
