package com.flashticket.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建员工请求（管理员设置初始密码，不返回明文）。
 *
 * <p>角色以 roleId 列表传入，越权规则由服务层校验（ADMIN 不能授予 SUPER_ADMIN）。
 * 「邀请设置密码」作为 P1 增强，M03 先落地管理员直设初始密码。
 */
public record CreateStaffRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名须为 4-32 位字母、数字或下划线")
        String username,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱长度超出限制")
        String email,

        @NotBlank(message = "昵称不能为空")
        @Size(min = 2, max = 32, message = "昵称长度须为 2-32 字符")
        String nickname,

        @NotBlank(message = "初始密码不能为空")
        @Size(min = 12, max = 64, message = "密码长度须为 12-64 位")
        String password,

        @NotEmpty(message = "至少分配一个角色")
        List<Long> roleIds) {
}
