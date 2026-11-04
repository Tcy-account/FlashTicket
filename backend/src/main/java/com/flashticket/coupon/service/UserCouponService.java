package com.flashticket.coupon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.coupon.dto.CouponLockResult;
import com.flashticket.coupon.entity.CouponTemplate;
import com.flashticket.coupon.entity.CouponUsage;
import com.flashticket.coupon.entity.UserCoupon;
import com.flashticket.coupon.enums.CouponApplicableScope;
import com.flashticket.coupon.enums.CouponType;
import com.flashticket.coupon.mapper.CouponTemplateMapper;
import com.flashticket.coupon.mapper.CouponUsageMapper;
import com.flashticket.coupon.mapper.UserCouponMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户优惠券领域服务（M13 P0-C）：锁券、优惠计算、核销、返券。
 *
 * <p>锁券原子性由 {@link UserCouponMapper#lock} 条件更新保证（AVAILABLE 且未过期才可锁，
 * 并发下仅一个事务成功），是「同一券只能被一个有效订单锁定」的最终保证。优惠金额服务端
 * 计算，客户端不可篡改。核销/返券与订单状态机（支付/关单）在同一本地事务内互斥转换。
 */
@Service
public class UserCouponService {

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper templateMapper;
    private final CouponUsageMapper usageMapper;
    private final ObjectMapper objectMapper;

    public UserCouponService(UserCouponMapper userCouponMapper,
                             CouponTemplateMapper templateMapper,
                             CouponUsageMapper usageMapper,
                             ObjectMapper objectMapper) {
        this.userCouponMapper = userCouponMapper;
        this.templateMapper = templateMapper;
        this.usageMapper = usageMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 锁券并计算优惠（建单事务内调用，须在建单后调用 {@link #recordUsage} 落 usage）。
     *
     * @param userId         用户 ID
     * @param couponId       用户优惠券 ID
     * @param eventId        订单活动 ID（适用范围校验）
     * @param tierId         订单票档 ID（适用范围校验）
     * @param originalAmount 优惠前订单金额（分）
     * @param now            当前时间
     * @return 锁券结果（含优惠金额与规则快照）；券不可用/不适用/未达门槛抛 BizException
     */
    public CouponLockResult lockAndCalculate(Long userId, Long couponId, Long eventId, Long tierId,
                                             long originalAmount, LocalDateTime now) {
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        }
        if (!coupon.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.COUPON_NOT_OWNED);
        }
        CouponTemplate template = templateMapper.selectById(coupon.getTemplateId());
        if (template == null) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        validateScope(template, eventId, tierId);

        // 条件锁券：AVAILABLE 且未过期才可锁（并发仅一个事务成功）
        if (userCouponMapper.lock(couponId, userId, now) == 0) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        // 服务端计算优惠（客户端不可篡改）
        long discount = calculateDiscount(template, originalAmount);
        if (discount <= 0) {
            throw new BizException(ErrorCode.COUPON_THRESHOLD_NOT_MET);
        }
        return new CouponLockResult(couponId, template.getId(), discount,
                buildSnapshot(template, coupon));
    }

    /** 落锁券 usage 记录（建单拿到 orderId 后调用，同一事务）。 */
    public void recordUsage(Long userCouponId, Long orderId, LocalDateTime now) {
        CouponUsage usage = new CouponUsage();
        usage.setUserCouponId(userCouponId);
        usage.setOrderId(orderId);
        usage.setStatus("LOCKED");
        usage.setLockedAt(now);
        usageMapper.insert(usage);
    }

    /** 核销（支付成功）：LOCKED → USED，与订单 PAID 同事务。 */
    public void use(Long userCouponId, Long orderId, LocalDateTime now) {
        userCouponMapper.use(userCouponId, now);
        usageMapper.markUsed(userCouponId, orderId, now);
    }

    /** 返券（关单）：LOCKED → AVAILABLE（未过期）/EXPIRED（已过期），与订单 CLOSED 同事务。 */
    public void release(Long userCouponId, Long orderId, LocalDateTime now) {
        userCouponMapper.release(userCouponId, now);
        usageMapper.markReleased(userCouponId, orderId, now);
    }

    /* ---------- 内部 ---------- */

    private void validateScope(CouponTemplate template, Long eventId, Long tierId) {
        CouponApplicableScope scope;
        try {
            scope = CouponApplicableScope.valueOf(template.getApplicableScope());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.COUPON_NOT_APPLICABLE);
        }
        boolean match = switch (scope) {
            case ALL -> true;
            case EVENT -> eventId != null && eventId.equals(template.getScopeRefId());
            case TIER -> tierId != null && tierId.equals(template.getScopeRefId());
        };
        if (!match) {
            throw new BizException(ErrorCode.COUPON_NOT_APPLICABLE);
        }
    }

    private long calculateDiscount(CouponTemplate template, long originalAmount) {
        if (CouponType.FULL_REDUCTION.name().equals(template.getType())) {
            if (originalAmount >= template.getThresholdCent()) {
                return Math.min(template.getDiscountCent(), originalAmount);
            }
            return 0;
        }
        // PERCENTAGE：原价 * 折扣基点 / 10000，封顶 maxDiscountCent
        long discount = originalAmount * template.getDiscountRate() / 10000;
        if (template.getMaxDiscountCent() != null && template.getMaxDiscountCent() > 0) {
            discount = Math.min(discount, template.getMaxDiscountCent());
        }
        return Math.min(discount, originalAmount);
    }

    private String buildSnapshot(CouponTemplate template, UserCoupon coupon) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("type", template.getType());
        snap.put("thresholdCent", template.getThresholdCent());
        snap.put("discountCent", template.getDiscountCent());
        snap.put("discountRate", template.getDiscountRate());
        snap.put("maxDiscountCent", template.getMaxDiscountCent());
        snap.put("couponNo", coupon.getCouponNo());
        snap.put("templateVersion", template.getVersion());
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化优惠快照失败", e);
        }
    }
}
