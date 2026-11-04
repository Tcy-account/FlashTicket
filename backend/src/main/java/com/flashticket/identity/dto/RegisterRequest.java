package com.flashticket.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（用户名 + 邮箱 + 密码 + 邮箱验证码）。
 *
 * <p>规则见 doc/用户与账号模块设计.md 3.1：用户名 4-32 位字母/数字/下划线；
 * 密码 12-64 位；验证码 6 位数字。确认密码由前端校验，后端不接收。
 */
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名须为 4-32 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message = "邮箱长度超出限制")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 12, max = 64, message = "密码长度须为 12-64 位")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码须为 6 位数字")
    private String verifyCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }
}
