package com.flashticket.inventory.service;

import com.flashticket.inventory.entity.Inventory;
import com.flashticket.inventory.enums.AdmissionResult;
import com.flashticket.inventory.mapper.InventoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 高速库存准入（M15-C，模式3）。
 *
 * <p>Redis 维护每个票档的可售数量快照，抢票受理前用 Lua 原子预减做「准入预检」：
 * 明显无货的请求在入口被快速拒绝，减少无效请求进入 MQ 与 MySQL。MySQL 仍是最终
 * 权威账本（reserve 条件扣减 available&gt;=q 保证不超卖），Redis 只是乐观预检，
 * 允许短暂不一致，由定时对账用 MySQL 权威值校准。
 *
 * <p>Redis 不可用/快照缺失时 fail-open 回退 MySQL（FALLBACK），不阻断交易正确性。
 */
@Service
public class InventoryAdmissionService {

    private static final Logger log = LoggerFactory.getLogger(InventoryAdmissionService.class);

    private static final String KEY_PREFIX = "ft:inventory:admission:";

    /** 预减脚本：KEYS[1]=available 快照，ARGV[1]=数量。返回 1 预减成功 / 0 库存不足 / -1 快照缺失。 */
    private static final DefaultRedisScript<Long> ADMIT_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local qty = tonumber(ARGV[1])
            local available = redis.call('GET', key)
            if not available then
                return -1
            end
            if tonumber(available) < qty then
                return 0
            end
            redis.call('DECRBY', key, qty)
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final InventoryMapper inventoryMapper;
    private final boolean enabled;

    public InventoryAdmissionService(StringRedisTemplate redis,
                                     InventoryMapper inventoryMapper,
                                     @Value("${flashticket.inventory.redis-admission.enabled:false}") boolean enabled) {
        this.redis = redis;
        this.inventoryMapper = inventoryMapper;
        this.enabled = enabled;
    }

    /** 是否启用 Redis 高速准入（模式3）。 */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 抢票准入预检：Lua 原子预减 Redis 快照。
     *
     * @return ADMIT（预减成功）/ SOLD_OUT（快照判定不足）/ FALLBACK（快照缺失或 Redis 异常）
     */
    public AdmissionResult tryAdmit(Long tierId, int quantity) {
        if (!enabled) {
            return AdmissionResult.FALLBACK;
        }
        try {
            Long result = redis.execute(ADMIT_SCRIPT, List.of(key(tierId)), String.valueOf(quantity));
            if (result == null) {
                return AdmissionResult.FALLBACK;
            }
            return switch (result.intValue()) {
                case 1 -> AdmissionResult.ADMIT;
                case 0 -> AdmissionResult.SOLD_OUT;
                default -> AdmissionResult.FALLBACK;
            };
        } catch (Exception e) {
            log.warn("Redis 库存准入不可用，回退 MySQL tierId={}: {}", tierId, e.getMessage());
            return AdmissionResult.FALLBACK;
        }
    }

    /** 对账：定时用 MySQL 权威 available 全量校准 Redis 快照（含首次建立）。 */
    @Scheduled(fixedDelayString = "${flashticket.inventory.redis-admission.reconcile-ms:10000}",
            initialDelayString = "${flashticket.inventory.redis-admission.reconcile-ms:10000}")
    public void reconcile() {
        if (!enabled) {
            return;
        }
        try {
            List<Inventory> inventories = inventoryMapper.selectList(null);
            for (Inventory inv : inventories) {
                redis.opsForValue().set(key(inv.getTierId()), String.valueOf(inv.getAvailable()));
            }
        } catch (Exception e) {
            log.warn("库存准入对账失败: {}", e.getMessage());
        }
    }

    private static String key(Long tierId) {
        return KEY_PREFIX + tierId;
    }
}
