package com.flashticket.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建/编辑演出活动请求。
 *
 * <p>发布时的必填校验（封面/详情/主办方/须知等）留待 M06 审核发布；
 * M05 草稿阶段仅校验基础字段。version 用于乐观并发控制（编辑时回传）。
 */
public record EventRequest(
        @NotBlank(message = "分类不能为空")
        @Pattern(regexp = "\\d+", message = "分类 ID 格式非法")
        String categoryId,

        @NotBlank(message = "演出名称不能为空")
        @Size(min = 5, max = 100, message = "演出名称须 5-100 字符")
        String title,

        @Size(max = 200, message = "副标题最长 200 字符")
        String subtitle,

        @Size(max = 512, message = "封面地址最长 512 字符")
        String coverUrl,

        @Size(max = 500, message = "简介最长 500 字符")
        String description,

        String detailContent,

        @Size(max = 100, message = "主办方名称最长 100 字符")
        String organizerName,

        String attendanceNotice,

        String refundPolicy,

        Long version) {
}
