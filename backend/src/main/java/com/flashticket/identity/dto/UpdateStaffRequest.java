package com.flashticket.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改员工请求（昵称/邮箱，用户名与密码不在此修改）。
 */
public record UpdateStaffRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(min = 2, max = 32, message = "昵称长度须为 2-32 字符")
        String nickname,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱长度超出限制")
        String email) {
}
