package com.flashticket.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建/编辑分类请求。
 *
 * <p>code 为稳定业务标识，格式：大写字母开头，后续大写字母/数字/下划线，长度 2-32。
 */
public record CategoryRequest(
        @NotBlank(message = "分类码不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "分类码须为大写字母开头，2-32 位大写字母/数字/下划线")
        String code,

        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称最长 64 字符")
        String name,

        @Size(max = 255, message = "分类说明最长 255 字符")
        String description,

        Integer sortOrder) {
}
