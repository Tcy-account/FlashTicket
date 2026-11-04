package com.flashticket.identity.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.identity.dto.ChangePasswordRequest;
import com.flashticket.identity.dto.UpdateProfileRequest;
import com.flashticket.identity.dto.UserProfileResponse;
import com.flashticket.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户个人中心入口（M02，需认证）。
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    /** 获取当前用户资料。 */
    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.ok(userService.getProfile(SecurityUtils.currentUserId()));
    }

    /** 修改当前用户资料（昵称/头像）。 */
    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(SecurityUtils.currentUserId(), request));
    }

    /** 修改密码（验证旧密码）。 */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(SecurityUtils.currentUserId(), SecurityUtils.currentSessionId(), request);
        return ApiResponse.ok();
    }
}
