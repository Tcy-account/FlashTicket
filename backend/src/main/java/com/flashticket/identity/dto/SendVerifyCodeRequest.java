package com.flashticket.identity.dto;

import com.flashticket.identity.enums.VerifyCodePurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发送邮箱验证码请求。
 */
public class SendVerifyCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message = "邮箱长度超出限制")
    private String email;

    @NotNull(message = "验证码用途不能为空")
    private VerifyCodePurpose purpose;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public VerifyCodePurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(VerifyCodePurpose purpose) {
        this.purpose = purpose;
    }
}
