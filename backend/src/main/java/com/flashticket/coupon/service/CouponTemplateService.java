package com.flashticket.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.coupon.dto.CouponTemplateRequest;
import com.flashticket.coupon.dto.CouponTemplateResponse;
import com.flashticket.coupon.entity.Campaign;
import com.flashticket.coupon.entity.CouponQuota;
import com.flashticket.coupon.entity.CouponTemplate;
import com.flashticket.coupon.enums.CouponApplicableScope;
import com.flashticket.coupon.enums.CouponType;
import com.flashticket.coupon.enums.CouponValidityType;
import com.flashticket.coupon.mapper.CampaignMapper;
import com.flashticket.coupon.mapper.CouponQuotaMapper;
import com.flashticket.coupon.mapper.CouponTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 优惠券模板编排服务（M13 P0-A）：创建、编辑、启用（初始化额度账本）、暂停、恢复、终止、查询。
 *
 * <p>模板状态机 DRAFT → ACTIVE → PAUSED/TERMINATED。启用后优惠规则关键字段冻结
 * （version 快照），防止已发券权益被篡改；启用时初始化三态发行账本 total=available。
 */
@Service
public class CouponTemplateService {

    private final CouponTemplateMapper templateMapper;
    private final CampaignMapper campaignMapper;
    private final CouponQuotaMapper quotaMapper;

    public CouponTemplateService(CouponTemplateMapper templateMapper,
                                 CampaignMapper campaignMapper,
                                 CouponQuotaMapper quotaMapper) {
        this.templateMapper = templateMapper;
        this.campaignMapper = campaignMapper;
        this.quotaMapper = quotaMapper;
    }

    /** 创建模板（DRAFT），校验关联活动、时间、有效期与规则字段。 */
    @Transactional
    public CouponTemplateResponse create(CouponTemplateRequest req) {
        requireCampaign(req.campaignId());
        validate(req);
        CouponTemplate t = new CouponTemplate();
        applyFields(t, req);
        t.setStatus("DRAFT");
        t.setVersion(0L);
        templateMapper.insert(t);
        return toResponse(t, null);
    }

    /** 编辑模板（仅 DRAFT，启用后规则冻结）。 */
    @Transactional
    public CouponTemplateResponse update(Long id, CouponTemplateRequest req) {
        CouponTemplate t = requireTemplate(id);
        if (!"DRAFT".equals(t.getStatus())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_FROZEN);
        }
        requireCampaign(req.campaignId());
        validate(req);
        applyFields(t, req);
        templateMapper.updateById(t);
        return toResponse(t, null);
    }

    /** 启用：DRAFT → ACTIVE + 初始化发行额度账本（幂等）。 */
    @Transactional
    public CouponTemplateResponse activate(Long id) {
        CouponTemplate t = requireTemplate(id);
        if (templateMapper.activate(id) == 0) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_ILLEGAL_STATE);
        }
        // 初始化额度账本（template_id 唯一，已存在则跳过）
        CouponQuota quota = quotaMapper.selectOne(
                new LambdaQueryWrapper<CouponQuota>().eq(CouponQuota::getTemplateId, id));
        if (quota == null) {
            quota = new CouponQuota();
            quota.setTemplateId(id);
            quota.setTotal(t.getTotalQuantity());
            quota.setAvailable(t.getTotalQuantity());
            quota.setReserved(0);
            quota.setIssued(0);
            quota.setVersion(0L);
            quotaMapper.insert(quota);
        }
        return toResponse(t, quota);
    }

    /** 暂停：ACTIVE → PAUSED。 */
    @Transactional
    public void pause(Long id) {
        if (templateMapper.pause(id) == 0) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_ILLEGAL_STATE);
        }
    }

    /** 恢复：PAUSED → ACTIVE。 */
    @Transactional
    public void resume(Long id) {
        if (templateMapper.resume(id) == 0) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_ILLEGAL_STATE);
        }
    }

    /** 终止：ACTIVE/PAUSED → TERMINATED。 */
    @Transactional
    public void terminate(Long id) {
        if (templateMapper.terminate(id) == 0) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_ILLEGAL_STATE);
        }
    }

    /** 后台分页列表（状态/活动筛选）。 */
    public PageResult<CouponTemplateResponse> list(long page, long pageSize,
                                                   String status, Long campaignId) {
        LambdaQueryWrapper<CouponTemplate> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(CouponTemplate::getStatus, status);
        }
        if (campaignId != null) {
            qw.eq(CouponTemplate::getCampaignId, campaignId);
        }
        qw.orderByDesc(CouponTemplate::getId);
        Page<CouponTemplate> p = new Page<>(page, pageSize);
        IPage<CouponTemplate> result = templateMapper.selectPage(p, qw);
        List<CouponTemplateResponse> items = result.getRecords().stream()
                .map(t -> toResponse(t, quotaOf(t.getId()))).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 详情（含额度概览）。 */
    public CouponTemplateResponse get(Long id) {
        CouponTemplate t = requireTemplate(id);
        return toResponse(t, quotaOf(id));
    }

    /* ---------- 内部 ---------- */

    private CouponTemplate requireTemplate(Long id) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        return t;
    }

    private void requireCampaign(Long campaignId) {
        Campaign c = campaignMapper.selectById(campaignId);
        if (c == null) {
            throw new BizException(ErrorCode.CAMPAIGN_NOT_FOUND);
        }
    }

    private CouponQuota quotaOf(Long templateId) {
        return quotaMapper.selectOne(
                new LambdaQueryWrapper<CouponQuota>().eq(CouponQuota::getTemplateId, templateId));
    }

    private void validate(CouponTemplateRequest req) {
        // 领取窗口
        if (req.claimEndAt() == null || !req.claimEndAt().isAfter(req.claimStartAt())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_TIME_INVALID, "领取结束时间必须晚于开始时间");
        }
        // 类型与规则字段
        CouponType type;
        try {
            type = CouponType.valueOf(req.type());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "优惠券类型非法");
        }
        if (type == CouponType.FULL_REDUCTION && (req.discountCent() == null || req.discountCent() <= 0)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "满减券必须填写正数的满减金额");
        }
        if (type == CouponType.PERCENTAGE
                && (req.discountRate() == null || req.discountRate() <= 0 || req.discountRate() > 10000)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "折扣券必须填写 0~10000 的折扣基点");
        }
        // 有效期
        CouponValidityType validity;
        try {
            validity = CouponValidityType.valueOf(req.validityType());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "有效期类型非法");
        }
        if (validity == CouponValidityType.FIXED
                && (req.validFrom() == null || req.validUntil() == null || !req.validUntil().isAfter(req.validFrom()))) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_TIME_INVALID, "固定有效期必须填写且结束晚于开始");
        }
        if (validity == CouponValidityType.RELATIVE_DAYS && (req.validDays() == null || req.validDays() <= 0)) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_TIME_INVALID, "领取后有效天数必须大于 0");
        }
        // 适用范围
        try {
            CouponApplicableScope.valueOf(req.applicableScope());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "适用范围非法");
        }
    }

    private void applyFields(CouponTemplate t, CouponTemplateRequest req) {
        t.setCampaignId(req.campaignId());
        t.setName(req.name().trim());
        t.setType(req.type());
        t.setThresholdCent(req.thresholdCent() == null ? 0L : req.thresholdCent());
        t.setDiscountCent(req.discountCent() == null ? 0L : req.discountCent());
        t.setDiscountRate(req.discountRate() == null ? 0 : req.discountRate());
        t.setMaxDiscountCent(req.maxDiscountCent() == null ? 0L : req.maxDiscountCent());
        t.setTotalQuantity(req.totalQuantity());
        t.setPerUserLimit(req.perUserLimit());
        t.setClaimStartAt(req.claimStartAt());
        t.setClaimEndAt(req.claimEndAt());
        t.setValidityType(req.validityType());
        t.setValidFrom(req.validFrom());
        t.setValidUntil(req.validUntil());
        t.setValidDays(req.validDays());
        t.setApplicableScope(req.applicableScope());
        t.setScopeRefId(req.scopeRefId());
    }

    private static CouponTemplateResponse toResponse(CouponTemplate t, CouponQuota q) {
        return new CouponTemplateResponse(
                t.getId(), t.getCampaignId(), t.getName(), t.getType(),
                t.getThresholdCent(), t.getDiscountCent(), t.getDiscountRate(), t.getMaxDiscountCent(),
                t.getTotalQuantity(), t.getPerUserLimit(), t.getClaimStartAt(), t.getClaimEndAt(),
                t.getValidityType(), t.getValidFrom(), t.getValidUntil(), t.getValidDays(),
                t.getApplicableScope(), t.getScopeRefId(), t.getStatus(), t.getVersion(),
                q == null ? null : q.getAvailable(), q == null ? null : q.getIssued(),
                t.getCreatedAt());
    }
}
