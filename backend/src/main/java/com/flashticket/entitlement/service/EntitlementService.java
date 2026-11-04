package com.flashticket.entitlement.service;

import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.entitlement.dto.EntitlementResponse;
import com.flashticket.entitlement.entity.Entitlement;
import com.flashticket.entitlement.enums.EntitlementStatus;
import com.flashticket.entitlement.mapper.EntitlementMapper;
import com.flashticket.order.entity.OrderItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 票务权益领域服务（M10）：支付成功后为购买数量生成有效门票权益。
 *
 * <p>一票一权益：订单明细每个购买数量生成一条独立权益，记录成交时的活动/
 * 场次/场馆/票档快照。(order_item_id, ticket_index) 唯一保证重复出票幂等。
 * 退款撤销权益在 M11 落地，本服务预留 status 字段（VALID/USED/REVOKED）。
 *
 * <p>本服务只读依赖 order 域 {@link OrderItem}（快照信息），不反向修改订单。
 */
@Service
public class EntitlementService {

    private final EntitlementMapper entitlementMapper;

    public EntitlementService(EntitlementMapper entitlementMapper) {
        this.entitlementMapper = entitlementMapper;
    }

    /**
     * 为支付成功的订单生成票务权益（幂等：已出票则直接返回既有权益）。
     *
     * @param orderId 订单 ID
     * @param userId  持有人用户 ID
     * @param items   订单明细（含交易快照与购买数量）
     * @return 本次生成（或幂等命中的既有）权益列表
     */
    @Transactional
    public List<Entitlement> issue(Long orderId, Long userId, List<OrderItem> items) {
        List<Entitlement> existing = entitlementMapper.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Entitlement> created = new ArrayList<>();
        for (OrderItem item : items) {
            for (int i = 1; i <= item.getQuantity(); i++) {
                Entitlement e = new Entitlement();
                e.setEntitlementNo(generateEntitlementNo());
                e.setOrderId(orderId);
                e.setOrderItemId(item.getId());
                e.setUserId(userId);
                e.setEventId(item.getEventId());
                e.setEventTitle(item.getEventTitle());
                e.setSessionId(item.getSessionId());
                e.setSessionStartAt(item.getSessionStartAt());
                e.setVenueName(item.getVenueName());
                e.setTierId(item.getTierId());
                e.setTierName(item.getTierName());
                e.setTicketIndex(i);
                e.setStatus(EntitlementStatus.VALID.name());
                e.setVersion(0L);
                e.setIssuedAt(now);
                entitlementMapper.insert(e);
                created.add(e);
            }
        }
        return created;
    }

    /** 我的门票列表（按出票时间倒序）。 */
    public List<EntitlementResponse> listMine(Long userId) {
        return entitlementMapper.findByUserId(userId).stream()
                .map(EntitlementService::toResponse)
                .toList();
    }

    /** 我的门票详情（归属校验，防止越权查看他人凭证）。 */
    public EntitlementResponse getMine(Long userId, Long entitlementId) {
        Entitlement e = entitlementMapper.selectById(entitlementId);
        if (e == null) {
            throw new BizException(ErrorCode.ENTITLEMENT_NOT_FOUND);
        }
        if (!e.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ENTITLEMENT_NOT_OWNED);
        }
        return toResponse(e);
    }

    /**
     * 撤销订单全部有效权益（M11 退款成功，同一本地事务内调用）。
     *
     * <p>仅将 VALID 权益置为 REVOKED，幂等：重复调用不重复更新已撤销权益。
     */
    public void revokeByOrderId(Long orderId) {
        entitlementMapper.revokeByOrderId(orderId, LocalDateTime.now(ZoneOffset.UTC));
    }

    /* ---------- 内部 ---------- */

    /** 权益编号：ET + 毫秒时间戳 + 4 位随机数（唯一约束兜底并发冲突）。 */
    private String generateEntitlementNo() {
        return "ET" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static EntitlementResponse toResponse(Entitlement e) {
        return new EntitlementResponse(
                String.valueOf(e.getId()), e.getEntitlementNo(),
                String.valueOf(e.getOrderId()), String.valueOf(e.getOrderItemId()),
                String.valueOf(e.getUserId()), String.valueOf(e.getEventId()),
                e.getEventTitle(), String.valueOf(e.getSessionId()), e.getSessionStartAt(),
                e.getVenueName(), String.valueOf(e.getTierId()), e.getTierName(),
                e.getTicketIndex(), e.getStatus(), e.getIssuedAt(), e.getCreatedAt());
    }
}
