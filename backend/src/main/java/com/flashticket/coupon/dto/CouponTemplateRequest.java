package com.flashticket.coupon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 创建/编辑优惠券模板请求（规则字段由服务端按 type 校验）。
 */
public record CouponTemplateRequest(
        @NotNull(message = "关联活动不能为空")
        Long campaignId,

        @NotBlank(message = "优惠券名称不能为空")
        @Size(max = 100, message = "优惠券名称最长 100 字符")
        String name,

        @NotBlank(message = "优惠券类型不能为空")
        String type,

        @Min(value = 0, message = "门槛金额不能为负")
        Long thresholdCent,

        @Min(value = 0, message = "满减金额不能为负")
        Long discountCent,

        @Min(value = 0, message = "折扣基点不能为负")
        @Max(value = 10000, message = "折扣基点不能超过 10000（原价）")
        Integer discountRate,

        @Min(value = 0, message = "最高优惠不能为负")
        Long maxDiscountCent,

        @NotNull(message = "发行总量不能为空")
        @Min(value = 1, message = "发行总量必须大于 0")
        Integer totalQuantity,

        @NotNull(message = "每人限领不能为空")
        @Min(value = 1, message = "每人限领必须大于 0")
        Integer perUserLimit,

        @NotNull(message = "领取开始时间不能为空")
        LocalDateTime claimStartAt,

        @NotNull(message = "领取结束时间不能为空")
        LocalDateTime claimEndAt,

        @NotBlank(message = "有效期类型不能为空")
        String validityType,

        LocalDateTime validFrom,

        LocalDateTime validUntil,

        @Min(value = 1, message = "领取后有效天数必须大于 0")
        Integer validDays,

        @NotBlank(message = "适用范围不能为空")
        String applicableScope,

        Long scopeRefId) {
}
