package com.flashticket.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.dto.PageResult;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.AuthUser;
import com.flashticket.coupon.dto.CampaignRequest;
import com.flashticket.coupon.dto.CampaignResponse;
import com.flashticket.coupon.dto.CampaignReviewRequest;
import com.flashticket.coupon.entity.Campaign;
import com.flashticket.coupon.enums.CampaignStatus;
import com.flashticket.coupon.enums.CampaignType;
import com.flashticket.coupon.mapper.CampaignMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 营销活动编排服务（M13 P0-A）：创建、编辑、提交审核、审核、结束/取消、查询。
 *
 * <p>状态机 DRAFT → PENDING_REVIEW → PUBLISHED → ENDED/CANCELLED，审核驳回回 DRAFT，
 * 审核人 ≠ 提交人（与 event 域审核一致）。活动结束不影响已发券，使用有效期由模板决定。
 */
@Service
public class CampaignService {

    private final CampaignMapper campaignMapper;

    public CampaignService(CampaignMapper campaignMapper) {
        this.campaignMapper = campaignMapper;
    }

    /** 创建活动（DRAFT）。 */
    @Transactional
    public CampaignResponse create(CampaignRequest req, AuthUser operator) {
        validateTime(req);
        validateType(req.type());
        Campaign c = new Campaign();
        c.setName(req.name().trim());
        c.setType(req.type());
        c.setDescription(trimToNull(req.description()));
        c.setStartAt(req.startAt());
        c.setEndAt(req.endAt());
        c.setStatus(CampaignStatus.DRAFT.name());
        c.setCreatedBy(operator.getUserId());
        c.setVersion(0L);
        campaignMapper.insert(c);
        return toResponse(c);
    }

    /** 编辑活动（仅 DRAFT）。 */
    @Transactional
    public CampaignResponse update(Long id, CampaignRequest req) {
        Campaign c = requireCampaign(id);
        if (!CampaignStatus.DRAFT.name().equals(c.getStatus())) {
            throw new BizException(ErrorCode.CAMPAIGN_ILLEGAL_STATE);
        }
        validateTime(req);
        validateType(req.type());
        c.setName(req.name().trim());
        c.setType(req.type());
        c.setDescription(trimToNull(req.description()));
        c.setStartAt(req.startAt());
        c.setEndAt(req.endAt());
        campaignMapper.updateById(c);
        return toResponse(c);
    }

    /** 提交审核：DRAFT → PENDING_REVIEW。 */
    @Transactional
    public void submitReview(Long id) {
        if (campaignMapper.submitReview(id) == 0) {
            throw new BizException(ErrorCode.CAMPAIGN_ILLEGAL_STATE);
        }
    }

    /** 审核：PENDING_REVIEW → PUBLISHED（通过）或 DRAFT（驳回，须填原因）。 */
    @Transactional
    public void review(Long id, CampaignReviewRequest req, AuthUser reviewer) {
        Campaign c = requireCampaign(id);
        if (reviewer.getUserId().equals(c.getCreatedBy())) {
            throw new BizException(ErrorCode.CAMPAIGN_SELF_REVIEW);
        }
        boolean approve = Boolean.TRUE.equals(req.approve());
        if (!approve && (req.comment() == null || req.comment().isBlank())) {
            throw new BizException(ErrorCode.CAMPAIGN_REVIEW_COMMENT_REQUIRED);
        }
        String target = approve ? CampaignStatus.PUBLISHED.name() : CampaignStatus.DRAFT.name();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (campaignMapper.review(id, target, reviewer.getUserId(), now) == 0) {
            throw new BizException(ErrorCode.CAMPAIGN_ILLEGAL_STATE);
        }
    }

    /** 结束/取消活动：PUBLISHED → ENDED/CANCELLED。 */
    @Transactional
    public void endOrCancel(Long id, String target) {
        if (campaignMapper.endOrCancel(id, target) == 0) {
            throw new BizException(ErrorCode.CAMPAIGN_ILLEGAL_STATE);
        }
    }

    /** 后台分页列表（状态筛选）。 */
    public PageResult<CampaignResponse> list(long page, long pageSize, String status) {
        LambdaQueryWrapper<Campaign> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(Campaign::getStatus, status);
        }
        qw.orderByDesc(Campaign::getId);
        Page<Campaign> p = new Page<>(page, pageSize);
        IPage<Campaign> result = campaignMapper.selectPage(p, qw);
        List<CampaignResponse> items = result.getRecords().stream()
                .map(CampaignService::toResponse).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 详情。 */
    public CampaignResponse get(Long id) {
        return toResponse(requireCampaign(id));
    }

    /* ---------- 内部 ---------- */

    private Campaign requireCampaign(Long id) {
        Campaign c = campaignMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CAMPAIGN_NOT_FOUND);
        }
        return c;
    }

    private void validateTime(CampaignRequest req) {
        if (req.endAt() == null || !req.endAt().isAfter(req.startAt())) {
            throw new BizException(ErrorCode.CAMPAIGN_TIME_INVALID);
        }
    }

    private void validateType(String type) {
        try {
            CampaignType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "活动类型非法");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static CampaignResponse toResponse(Campaign c) {
        return new CampaignResponse(
                c.getId(), c.getName(), c.getType(), c.getDescription(),
                c.getStartAt(), c.getEndAt(), c.getStatus(), c.getCreatedBy(),
                c.getReviewedBy(), c.getReviewedAt(), c.getVersion(), c.getCreatedAt());
    }
}
