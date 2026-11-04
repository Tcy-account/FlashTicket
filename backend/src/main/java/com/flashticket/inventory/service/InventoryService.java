package com.flashticket.inventory.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.event.entity.TicketTier;
import com.flashticket.event.mapper.TicketTierMapper;
import com.flashticket.inventory.dto.InventoryLedgerResponse;
import com.flashticket.inventory.dto.InventoryResponse;
import com.flashticket.inventory.dto.ReservationResponse;
import com.flashticket.inventory.entity.Inventory;
import com.flashticket.inventory.entity.InventoryLedger;
import com.flashticket.inventory.entity.PurchaseLimit;
import com.flashticket.inventory.entity.Reservation;
import com.flashticket.inventory.enums.InventoryOperation;
import com.flashticket.inventory.enums.ReservationStatus;
import com.flashticket.inventory.mapper.InventoryLedgerMapper;
import com.flashticket.inventory.mapper.InventoryMapper;
import com.flashticket.inventory.mapper.PurchaseLimitMapper;
import com.flashticket.inventory.mapper.ReservationMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 库存领域服务（M07）：库存初始化、数据库预占、确认、释放、查询与流水。
 *
 * <p>这是 FlashTicket 防超卖的最终安全边界（见 doc/库存与票档预占模块.md 八）：
 * <ul>
 *   <li>三态守恒 total = available + reserved + sold，由条件更新保证非负与守恒；</li>
 *   <li>预占 reserve 用「条件扣减 available + 唯一 request_id + 限购 FOR UPDATE」；</li>
 *   <li>确认 confirm / 释放 release 用条件状态转换，重复操作幂等返回；</li>
 *   <li>库存更新、预占状态变更、限购变更、流水写入在同一本地事务完成。</li>
 * </ul>
 *
 * <p>销售窗口/活动状态等业务资格校验由抢票编排层（M08 PurchaseCheckoutService）
 * 负责，本模块只保证库存账本与限购的正确性（模块边界见 doc/跨域审查 6.1）。
 * 本模块依赖票档只读配置（per_user_limit / min_purchase / max_purchase），
 * 属模块化单体内的合理只读引用，不反向修改票档。
 */
@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final ReservationMapper reservationMapper;
    private final InventoryLedgerMapper ledgerMapper;
    private final PurchaseLimitMapper purchaseLimitMapper;
    private final TicketTierMapper tierMapper;

    public InventoryService(InventoryMapper inventoryMapper,
                            ReservationMapper reservationMapper,
                            InventoryLedgerMapper ledgerMapper,
                            PurchaseLimitMapper purchaseLimitMapper,
                            TicketTierMapper tierMapper) {
        this.inventoryMapper = inventoryMapper;
        this.reservationMapper = reservationMapper;
        this.ledgerMapper = ledgerMapper;
        this.purchaseLimitMapper = purchaseLimitMapper;
        this.tierMapper = tierMapper;
    }

    /**
     * 初始化库存账本（发布时调用，幂等）。
     *
     * <p>total = available = 初始票量，reserved = sold = 0。已存在的账本直接跳过，
     * 保证发布/重试不重复初始化（见 doc/库存与票档预占模块.md 三·库存初始化）。
     */
    @Transactional
    public void initialize(Long tierId, int total) {
        if (inventoryMapper.selectById(tierId) != null) {
            return;
        }
        Inventory inventory = new Inventory();
        inventory.setTierId(tierId);
        inventory.setTotal(total);
        inventory.setAvailable(total);
        inventory.setReserved(0);
        inventory.setSold(0);
        inventory.setVersion(0L);
        inventory.setGeneration(1L);
        try {
            inventoryMapper.insert(inventory);
        } catch (DuplicateKeyException e) {
            return; // 并发初始化，忽略
        }
        writeLedger(tierId, null, InventoryOperation.INIT, total);
    }

    /**
     * 数据库预占：原子扣减可售，占用限购名额，生成 RESERVED 预占记录。
     *
     * @param requestId 抢票请求幂等键（重复请求只返回原状态，不重复扣减）
     * @param tierId    票档 ID
     * @param userId    用户 ID
     * @param quantity  购买数量
     * @param expireAt  预占过期时间（订单待支付期限，由抢票编排层决定）
     * @return 预占记录（新创建或幂等命中的既有记录）
     */
    @Transactional
    public Reservation reserve(String requestId, Long tierId, Long userId,
                               int quantity, LocalDateTime expireAt) {
        // 幂等：同一 requestId 重复提交直接返回原预占
        Reservation existing = reservationMapper.findByRequestId(requestId);
        if (existing != null) {
            return existing;
        }

        TicketTier tier = requireTier(tierId);
        assertQuantity(tier, quantity);
        requireInventory(tierId);

        // 1. 条件扣减可售库存（防超卖，最终安全边界）
        if (inventoryMapper.reserve(tierId, quantity) == 0) {
            throw new BizException(ErrorCode.INVENTORY_SOLD_OUT);
        }

        // 2. 占用限购名额（FOR UPDATE 串行化，防突破每人限购）
        occupyLimit(userId, tierId, quantity, tier.getPerUserLimit());

        // 3. 落预占记录（request_id 唯一约束兜底，防并发同请求重复扣减）
        Reservation reservation = new Reservation();
        reservation.setRequestId(requestId);
        reservation.setTierId(tierId);
        reservation.setUserId(userId);
        reservation.setQuantity(quantity);
        reservation.setStatus(ReservationStatus.RESERVED.name());
        reservation.setExpireAt(expireAt);
        reservation.setVersion(0L);
        reservationMapper.insert(reservation);

        // 4. 写流水
        writeLedger(tierId, reservation.getId(), InventoryOperation.RESERVE, quantity);
        return reservation;
    }

    /**
     * 确认已售：RESERVED → CONFIRMED，reserved 减少、sold 增加。
     *
     * <p>重复确认幂等返回；已释放预占不可再确认。
     */
    @Transactional
    public Reservation confirm(String requestId) {
        Reservation reservation = requireReservationByRequestId(requestId);
        if (ReservationStatus.CONFIRMED.name().equals(reservation.getStatus())) {
            return reservation; // 幂等
        }
        if (!ReservationStatus.RESERVED.name().equals(reservation.getStatus())) {
            throw new BizException(ErrorCode.INVENTORY_RESERVATION_ILLEGAL_TRANSITION);
        }

        // 条件状态转换：仅 RESERVED 可转 CONFIRMED（与释放互斥，只有一个终态）
        if (reservationMapper.confirm(reservation.getId()) == 0) {
            return recheckAfterRace(reservation.getId());
        }
        // 库存：reserved → sold
        if (inventoryMapper.confirm(reservation.getTierId(), reservation.getQuantity()) == 0) {
            throw new BizException(ErrorCode.INVENTORY_RESERVATION_ILLEGAL_TRANSITION);
        }
        // 限购：reserved → sold
        purchaseLimitMapper.moveReservedToSold(
                reservation.getUserId(), reservation.getTierId(), reservation.getQuantity());
        writeLedger(reservation.getTierId(), reservation.getId(),
                InventoryOperation.CONFIRM, reservation.getQuantity());
        return reservationMapper.selectById(reservation.getId());
    }

    /**
     * 释放预占：RESERVED → RELEASED，reserved 减少、available 增加。
     *
     * <p>重复释放幂等返回；已确认已售的预占禁止普通释放（退款回补走独立业务）。
     */
    @Transactional
    public Reservation release(String requestId) {
        Reservation reservation = requireReservationByRequestId(requestId);
        if (ReservationStatus.RELEASED.name().equals(reservation.getStatus())) {
            return reservation; // 幂等
        }
        if (!ReservationStatus.RESERVED.name().equals(reservation.getStatus())) {
            throw new BizException(ErrorCode.INVENTORY_RESERVATION_ILLEGAL_TRANSITION);
        }

        if (reservationMapper.release(reservation.getId()) == 0) {
            return recheckAfterRace(reservation.getId());
        }
        if (inventoryMapper.release(reservation.getTierId(), reservation.getQuantity()) == 0) {
            throw new BizException(ErrorCode.INVENTORY_RESERVATION_ILLEGAL_TRANSITION);
        }
        purchaseLimitMapper.reduceReserved(
                reservation.getUserId(), reservation.getTierId(), reservation.getQuantity());
        writeLedger(reservation.getTierId(), reservation.getId(),
                InventoryOperation.RELEASE, reservation.getQuantity());
        return reservationMapper.selectById(reservation.getId());
    }

    /* ---------- 查询 ---------- */

    /** 库存总账（后台 inventory:read）。 */
    public InventoryResponse getInventory(Long tierId) {
        return toResponse(requireInventory(tierId));
    }

    /** 查询库存账本（未初始化返回 null，供公开可售状态展示容错）。 */
    public InventoryResponse getInventoryOrNull(Long tierId) {
        Inventory inventory = inventoryMapper.selectById(tierId);
        return inventory == null ? null : toResponse(inventory);
    }

    /** 某票档的全部预占记录（后台 inventory:read）。 */
    public List<ReservationResponse> listReservations(Long tierId) {
        return reservationMapper.findByTierId(tierId).stream()
                .map(InventoryService::toReservationResponse).toList();
    }

    /** 某票档的库存流水（后台 inventory:read）。 */
    public List<InventoryLedgerResponse> listLedger(Long tierId) {
        return ledgerMapper.findByTierId(tierId).stream()
                .map(InventoryService::toLedgerResponse).toList();
    }

    /* ---------- 内部 ---------- */

    /**
     * 确保限购行存在并加锁（FOR UPDATE），返回锁定行。
     *
     * <p>行不存在时先插入占位零行，并发插入撞唯一约束则重查加锁；这是
     * 「先确保存在 + 行级锁 + 条件更新」的原子限购占用（见 C03 锁顺序）。
     */
    private PurchaseLimit acquireLimitRow(Long userId, Long tierId) {
        PurchaseLimit limit = purchaseLimitMapper.selectForUpdate(userId, tierId);
        if (limit != null) {
            return limit;
        }
        PurchaseLimit row = new PurchaseLimit();
        row.setUserId(userId);
        row.setTierId(tierId);
        row.setReservedQuantity(0);
        row.setSoldQuantity(0);
        row.setVersion(0L);
        try {
            purchaseLimitMapper.insert(row);
            return row;
        } catch (DuplicateKeyException e) {
            return purchaseLimitMapper.selectForUpdate(userId, tierId);
        }
    }

    /** 校验限购名额并原子占用 reserved 名额。 */
    private void occupyLimit(Long userId, Long tierId, int quantity, int perUserLimit) {
        PurchaseLimit limit = acquireLimitRow(userId, tierId);
        int used = limit.getReservedQuantity() + limit.getSoldQuantity();
        if (used + quantity > perUserLimit) {
            throw new BizException(ErrorCode.INVENTORY_LIMIT_EXCEEDED);
        }
        purchaseLimitMapper.addReserved(userId, tierId, quantity);
    }

    /** 条件更新受影响行数为 0 时的并发兜底：重读后按终态返回或抛冲突。 */
    private Reservation recheckAfterRace(Long reservationId) {
        Reservation latest = reservationMapper.selectById(reservationId);
        if (latest != null
                && !ReservationStatus.RESERVED.name().equals(latest.getStatus())) {
            return latest;
        }
        throw new BizException(ErrorCode.INVENTORY_RESERVATION_ILLEGAL_TRANSITION);
    }

    private void writeLedger(Long tierId, Long reservationId,
                             InventoryOperation operation, int quantity) {
        InventoryLedger ledger = new InventoryLedger();
        ledger.setOperationId(UUID.randomUUID().toString());
        ledger.setTierId(tierId);
        ledger.setReservationId(reservationId);
        ledger.setOperation(operation.name());
        ledger.setQuantity(quantity);
        ledgerMapper.insert(ledger);
    }

    private TicketTier requireTier(Long tierId) {
        TicketTier tier = tierMapper.selectById(tierId);
        if (tier == null) {
            throw new BizException(ErrorCode.EVENT_TIER_NOT_FOUND);
        }
        return tier;
    }

    private Inventory requireInventory(Long tierId) {
        Inventory inventory = inventoryMapper.selectById(tierId);
        if (inventory == null) {
            throw new BizException(ErrorCode.INVENTORY_NOT_FOUND);
        }
        return inventory;
    }

    private Reservation requireReservationByRequestId(String requestId) {
        Reservation reservation = reservationMapper.findByRequestId(requestId);
        if (reservation == null) {
            throw new BizException(ErrorCode.INVENTORY_RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    private void assertQuantity(TicketTier tier, int quantity) {
        if (quantity < tier.getMinPurchase() || quantity > tier.getMaxPurchase()) {
            throw new BizException(ErrorCode.INVENTORY_QUANTITY_INVALID);
        }
    }

    private static InventoryResponse toResponse(Inventory inv) {
        return new InventoryResponse(
                String.valueOf(inv.getTierId()), inv.getTotal(), inv.getAvailable(),
                inv.getReserved(), inv.getSold(), inv.getVersion(), inv.getGeneration(),
                inv.getUpdatedAt());
    }

    private static ReservationResponse toReservationResponse(Reservation r) {
        return new ReservationResponse(
                String.valueOf(r.getId()), r.getRequestId(), String.valueOf(r.getTierId()),
                String.valueOf(r.getUserId()), r.getQuantity(), r.getStatus(), r.getExpireAt(),
                r.getVersion(), r.getCreatedAt());
    }

    private static InventoryLedgerResponse toLedgerResponse(InventoryLedger l) {
        return new InventoryLedgerResponse(
                String.valueOf(l.getId()), l.getOperationId(), String.valueOf(l.getTierId()),
                l.getReservationId() == null ? null : String.valueOf(l.getReservationId()),
                l.getOperation(), l.getQuantity(), l.getCreatedAt());
    }
}
