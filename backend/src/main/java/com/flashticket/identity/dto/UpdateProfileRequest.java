package com.flashticket.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改资料请求：昵称 + 头像地址。
 */
public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(min = 2, max = 32, message = "昵称长度需为 2-32 字符")
        String nickname,

        @Size(max = 255, message = "头像地址过长")
        String avatarUrl) {
}
