package com.flashticket.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.coupon.dto.ClaimResult;
import com.flashticket.coupon.dto.CouponAvailableResponse;
import com.flashticket.coupon.dto.UserCouponResponse;
import com.flashticket.coupon.entity.Campaign;
import com.flashticket.coupon.entity.CouponClaimRequest;
import com.flashticket.coupon.entity.CouponLedger;
import com.flashticket.coupon.entity.CouponQuota;
import com.flashticket.coupon.entity.CouponTemplate;
import com.flashticket.coupon.entity.CouponUserQuota;
import com.flashticket.coupon.entity.UserCoupon;
import com.flashticket.coupon.enums.CampaignStatus;
import com.flashticket.coupon.enums.ClaimStatus;
import com.flashticket.coupon.enums.CouponOperation;
import com.flashticket.coupon.enums.CouponValidityType;
import com.flashticket.coupon.enums.UserCouponStatus;
import com.flashticket.coupon.mapper.CampaignMapper;
import com.flashticket.coupon.mapper.CouponClaimRequestMapper;
import com.flashticket.coupon.mapper.CouponLedgerMapper;
import com.flashticket.coupon.mapper.CouponQuotaMapper;
import com.flashticket.coupon.mapper.CouponTemplateMapper;
import com.flashticket.coupon.mapper.CouponUserQuotaMapper;
import com.flashticket.coupon.mapper.UserCouponMapper;
import com.flashticket.risk.service.RateLimitService;
import com.flashticket.risk.service.RiskRestrictionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 领券领域服务（M13 P0-B）：MySQL 同步领券正确性基线。
 *
 * <p>同一本地事务内完成：request_id 幂等占位 → 发行额度原子扣减（防超发）→
 * 用户限领 FOR UPDATE（防超领）→ 创建用户券 → 审计流水 → 请求标记成功。
 * 任一环节失败整体回滚，保证「不超发、不超领、不重复发券」。
 *
 * <p>request_id 唯一约束兜底并发重复：同请求并发提交时，第二个事务在插入
 * 请求记录处撞唯一约束而回滚，不会重复扣减额度（与 inventory reserve 同款模式）。
 */
@Service
public class CouponClaimService {

    private final CouponClaimRequestMapper claimRequestMapper;
    private final CouponTemplateMapper templateMapper;
    private final CampaignMapper campaignMapper;
    private final CouponQuotaMapper quotaMapper;
    private final CouponUserQuotaMapper userQuotaMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponLedgerMapper ledgerMapper;
    private final RateLimitService rateLimitService;
    private final RiskRestrictionService riskRestrictionService;

    public CouponClaimService(CouponClaimRequestMapper claimRequestMapper,
                              CouponTemplateMapper templateMapper,
                              CampaignMapper campaignMapper,
                              CouponQuotaMapper quotaMapper,
                              CouponUserQuotaMapper userQuotaMapper,
                              UserCouponMapper userCouponMapper,
                              CouponLedgerMapper ledgerMapper,
                              RateLimitService rateLimitService,
                              RiskRestrictionService riskRestrictionService) {
        this.claimRequestMapper = claimRequestMapper;
        this.templateMapper = templateMapper;
        this.campaignMapper = campaignMapper;
        this.quotaMapper = quotaMapper;
        this.userQuotaMapper = userQuotaMapper;
        this.userCouponMapper = userCouponMapper;
        this.ledgerMapper = ledgerMapper;
        this.rateLimitService = rateLimitService;
        this.riskRestrictionService = riskRestrictionService;
    }

    /**
     * 提交领券（同步发券）。
     *
     * @param requestId  领券请求号（客户端生成，幂等键）
     * @param templateId 优惠券模板 ID
     * @param userId     用户 ID
     * @return 领券结果（SUCCEEDED 携带用户券，幂等命中返回原结果）
     */
    @Transactional
    public ClaimResult claim(String requestId, Long templateId, Long userId) {
        // 1. 幂等：同 requestId 重复提交直接返回原结果
        CouponClaimRequest existing = claimRequestMapper.findByRequestId(requestId);
        if (existing != null) {
            return toClaimResult(existing);
        }

        // 1.5 限流：同账号同模板窗口内最多 N 个新请求（M15-A，幂等重试在步骤 1 已短路）
        rateLimitService.checkClaim(userId, templateId);

        // 1.6 风控限制检查：被临时限制的用户禁止领券（M15-B）
        riskRestrictionService.checkRestriction("USER", String.valueOf(userId), "CLAIM");

        // 2. 资格校验（模板可用 + 活动已发布 + 领取窗口内）
        CouponTemplate template = requireTemplate(templateId);
        Campaign campaign = requireCampaign(template.getCampaignId());
        validateClaimable(template, campaign);

        // 3. 插入领券请求占位（request_id 唯一兜底并发幂等）
        CouponClaimRequest cr = new CouponClaimRequest();
        cr.setRequestId(requestId);
        cr.setUserId(userId);
        cr.setTemplateId(templateId);
        cr.setStatus(ClaimStatus.QUEUED.name());
        cr.setVersion(0L);
        try {
            claimRequestMapper.insert(cr);
        } catch (DuplicateKeyException e) {
            return toClaimResult(claimRequestMapper.findByRequestId(requestId));
        }

        // 4. 发行额度原子扣减（防超发）
        if (quotaMapper.deductIssue(templateId) == 0) {
            throw new BizException(ErrorCode.COUPON_SOLD_OUT);
        }

        // 5. 用户限领占用（FOR UPDATE 串行化，防超领）
        occupyUserQuota(userId, templateId, template.getPerUserLimit());

        // 6. 创建用户优惠券
        UserCoupon coupon = buildCoupon(userId, template, campaign);
        userCouponMapper.insert(coupon);

        // 7. 审计流水
        writeLedger(coupon, userId, CouponOperation.CLAIM, -1);

        // 8. 标记请求成功
        claimRequestMapper.markSucceeded(requestId, coupon.getId());

        return successResult(requestId, coupon, template);
    }

    /** 查询领券结果。 */
    public ClaimResult getClaimResult(String requestId, Long userId) {
        CouponClaimRequest cr = claimRequestMapper.findByRequestId(requestId);
        if (cr == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "领券请求不存在");
        }
        if (!cr.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.COUPON_NOT_OWNED);
        }
        return toClaimResult(cr);
    }

    /** 我的优惠券列表（按状态过滤，可空=全部）。 */
    public List<UserCouponResponse> listMine(Long userId, String status) {
        LambdaQueryWrapper<UserCoupon> qw = new LambdaQueryWrapper<>();
        qw.eq(UserCoupon::getUserId, userId);
        if (status != null && !status.isBlank()) {
            qw.eq(UserCoupon::getStatus, status);
        }
        qw.orderByDesc(UserCoupon::getId);
        return userCouponMapper.selectList(qw).stream()
                .map(this::toUserCouponResponse).toList();
    }

    /** 可领优惠券列表（公开）：模板 ACTIVE + 活动 PUBLISHED + 领取窗口内 + 额度有余。 */
    public List<CouponAvailableResponse> listAvailable() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<CouponTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStatus, "ACTIVE")
                        .orderByDesc(CouponTemplate::getId));
        return templates.stream()
                .filter(t -> now.isBefore(t.getClaimEndAt()) && !now.isBefore(t.getClaimStartAt()))
                .map(t -> toAvailableResponse(t, now))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /* ---------- 内部 ---------- */

    /** 确保用户限领行存在并加锁（FOR UPDATE），校验限领并累加。 */
    private void occupyUserQuota(Long userId, Long templateId, int perUserLimit) {
        CouponUserQuota uq = acquireUserQuotaRow(userId, templateId);
        if (uq.getClaimedCount() >= perUserLimit) {
            throw new BizException(ErrorCode.COUPON_LIMIT_EXCEEDED);
        }
        userQuotaMapper.incrementClaimed(userId, templateId);
    }

    /** 「先确保存在 + FOR UPDATE + 唯一约束兜底」的限领行锁定（同 inventory 模式）。 */
    private CouponUserQuota acquireUserQuotaRow(Long userId, Long templateId) {
        CouponUserQuota uq = userQuotaMapper.selectForUpdate(userId, templateId);
        if (uq != null) {
            return uq;
        }
        CouponUserQuota row = new CouponUserQuota();
        row.setUserId(userId);
        row.setTemplateId(templateId);
        row.setClaimedCount(0);
        row.setVersion(0L);
        try {
            userQuotaMapper.insert(row);
            return row;
        } catch (DuplicateKeyException e) {
            return userQuotaMapper.selectForUpdate(userId, templateId);
        }
    }

    private void validateClaimable(CouponTemplate template, Campaign campaign) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BizException(ErrorCode.COUPON_NOT_CLAIMABLE);
        }
        if (!CampaignStatus.PUBLISHED.name().equals(campaign.getStatus())) {
            throw new BizException(ErrorCode.COUPON_NOT_CLAIMABLE);
        }
        if (now.isBefore(template.getClaimStartAt()) || now.isAfter(template.getClaimEndAt())) {
            throw new BizException(ErrorCode.COUPON_CLAIM_WINDOW_NOT_OPEN);
        }
        if (now.isBefore(campaign.getStartAt()) || now.isAfter(campaign.getEndAt())) {
            throw new BizException(ErrorCode.COUPON_CLAIM_WINDOW_NOT_OPEN);
        }
    }

    private UserCoupon buildCoupon(Long userId, CouponTemplate template, Campaign campaign) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UserCoupon c = new UserCoupon();
        c.setUserId(userId);
        c.setTemplateId(template.getId());
        c.setCampaignId(campaign.getId());
        c.setCouponNo("CP" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        c.setStatus(UserCouponStatus.AVAILABLE.name());
        if (CouponValidityType.RELATIVE_DAYS.name().equals(template.getValidityType())) {
            c.setValidFrom(now);
            c.setValidUntil(now.plusDays(template.getValidDays()));
        } else {
            c.setValidFrom(template.getValidFrom());
            c.setValidUntil(template.getValidUntil());
        }
        c.setVersion(0L);
        return c;
    }

    private void writeLedger(UserCoupon coupon, Long userId, CouponOperation operation, int amount) {
        CouponLedger ledger = new CouponLedger();
        ledger.setOperationId(UUID.randomUUID().toString());
        ledger.setUserCouponId(coupon.getId());
        ledger.setTemplateId(coupon.getTemplateId());
        ledger.setUserId(userId);
        ledger.setOperation(operation.name());
        ledger.setAmount(amount);
        ledgerMapper.insert(ledger);
    }

    private CouponTemplate requireTemplate(Long id) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        return t;
    }

    /** 将模板映射为可领券列表项；活动未发布/窗口未开/额度无余则返回 null。 */
    private CouponAvailableResponse toAvailableResponse(CouponTemplate t, LocalDateTime now) {
        Campaign campaign = campaignMapper.selectById(t.getCampaignId());
        if (campaign == null || !CampaignStatus.PUBLISHED.name().equals(campaign.getStatus())) {
            return null;
        }
        if (now.isBefore(campaign.getStartAt()) || now.isAfter(campaign.getEndAt())) {
            return null;
        }
        CouponQuota quota = quotaMapper.selectOne(
                new LambdaQueryWrapper<CouponQuota>().eq(CouponQuota::getTemplateId, t.getId()));
        if (quota == null || quota.getAvailable() == null || quota.getAvailable() <= 0) {
            return null;
        }
        return new CouponAvailableResponse(
                t.getId(), t.getCampaignId(), campaign.getName(), t.getName(), t.getType(),
                t.getThresholdCent(), t.getDiscountCent(), t.getDiscountRate(), t.getMaxDiscountCent(),
                t.getTotalQuantity(), quota.getAvailable(), t.getPerUserLimit(),
                t.getClaimStartAt(), t.getClaimEndAt(),
                t.getValidityType(), t.getValidFrom(), t.getValidUntil(), t.getValidDays(),
                t.getApplicableScope(), t.getScopeRefId());
    }

    private Campaign requireCampaign(Long id) {
        Campaign c = campaignMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CAMPAIGN_NOT_FOUND);
        }
        return c;
    }

    private ClaimResult toClaimResult(CouponClaimRequest cr) {
        if (ClaimStatus.SUCCEEDED.name().equals(cr.getStatus()) && cr.getUserCouponId() != null) {
            UserCoupon coupon = userCouponMapper.selectById(cr.getUserCouponId());
            CouponTemplate template = templateMapper.selectById(cr.getTemplateId());
            return successResult(cr.getRequestId(), coupon, template);
        }
        return new ClaimResult(cr.getRequestId(), cr.getStatus(), null, cr.getFailReason(), null);
    }

    private ClaimResult successResult(String requestId, UserCoupon coupon, CouponTemplate template) {
        return new ClaimResult(requestId, ClaimStatus.SUCCEEDED.name(),
                coupon.getId(), null, toUserCouponResponse(coupon, template));
    }

    private UserCouponResponse toUserCouponResponse(UserCoupon c) {
        return toUserCouponResponse(c, templateMapper.selectById(c.getTemplateId()));
    }

    private static UserCouponResponse toUserCouponResponse(UserCoupon c, CouponTemplate t) {
        return new UserCouponResponse(
                c.getId(), c.getTemplateId(), c.getCampaignId(), c.getCouponNo(), c.getStatus(),
                c.getValidFrom(), c.getValidUntil(),
                t == null ? null : t.getName(),
                t == null ? null : t.getType(),
                t == null ? null : t.getThresholdCent(),
                t == null ? null : t.getDiscountCent(),
                t == null ? null : t.getDiscountRate(),
                t == null ? null : t.getMaxDiscountCent(),
                t == null ? null : t.getApplicableScope(),
                c.getUsedAt(), c.getLockedAt(), c.getCreatedAt());
    }
}
