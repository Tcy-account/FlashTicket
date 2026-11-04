package com.flashticket.purchase.service;

import com.flashticket.purchase.entity.PurchaseRequest;
import com.flashticket.purchase.mapper.PurchaseRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 抢票请求恢复扫描器（M12）：周期性扫描「结果不确定」的请求并驱动恢复。
 *
 * <p>扫描三类请求，逐一调用 {@link PurchaseRecoveryService}（每个请求独立事务，
 * 单个失败不影响整批）：
 * <ol>
 *   <li>租约过期的 PROCESSING（消费者崩溃）；</li>
 *   <li>到重试时间的 RECOVERING（技术故障）；</li>
 *   <li>超过受理截止时间仍未处理的 QUEUED（排队超时 → FAILED）。</li>
 * </ol>
 *
 * <p>test profile 下关闭调度，避免集成测试期间异步干扰；恢复正确性由直接调用
 * {@link PurchaseRecoveryService} 验证。
 */
@Component
@Profile("!test")
public class PurchaseRecoveryScanner {

    private static final Logger log = LoggerFactory.getLogger(PurchaseRecoveryScanner.class);

    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRecoveryService recoveryService;
    private final int batchSize;

    public PurchaseRecoveryScanner(PurchaseRequestMapper requestMapper,
                                   PurchaseRecoveryService recoveryService,
                                   @Value("${flashticket.purchase.recovery-batch:100}") int batchSize) {
        this.requestMapper = requestMapper;
        this.recoveryService = recoveryService;
        this.batchSize = batchSize;
    }

    /** 固定延迟扫描（首轮应用启动后立即执行一次）。 */
    @Scheduled(fixedDelayString = "${flashticket.purchase.recovery-scan-delay-ms:5000}",
            initialDelayString = "${flashticket.purchase.recovery-scan-delay-ms:5000}")
    public void scan() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<PurchaseRequest> leaseExpired = requestMapper.findLeaseExpired(now, batchSize);
        for (PurchaseRequest pr : leaseExpired) {
            try {
                recoveryService.recoverLeaseExpired(pr.getRequestId());
            } catch (Exception e) {
                log.warn("租约过期恢复失败 requestId={}: {}", pr.getRequestId(), e.getMessage());
            }
        }

        List<PurchaseRequest> recoverable = requestMapper.findRecoverable(now, batchSize);
        for (PurchaseRequest pr : recoverable) {
            try {
                recoveryService.recover(pr.getRequestId());
            } catch (Exception e) {
                log.warn("可恢复请求补发失败 requestId={}: {}", pr.getRequestId(), e.getMessage());
            }
        }

        List<PurchaseRequest> deadlineExpired = requestMapper.findDeadlineExpired(now, batchSize);
        for (PurchaseRequest pr : deadlineExpired) {
            try {
                recoveryService.failQueueTimeout(pr.getRequestId());
            } catch (Exception e) {
                log.warn("排队超时标记失败 requestId={}: {}", pr.getRequestId(), e.getMessage());
            }
        }
    }
}
