package com.flashticket.entitlement.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.entitlement.dto.EntitlementResponse;
import com.flashticket.entitlement.service.EntitlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端票务权益入口（M10）。
 *
 * <p>仅登录用户可访问，且只能查看本人门票（归属校验）。权益由支付成功驱动生成，
 * 不对用户端开放绕过支付的出票入口（见 doc/模拟支付与退款模块.md 九）。
 */
@RestController
@RequestMapping("/api/v1/entitlements")
public class EntitlementController {

    private final EntitlementService entitlementService;

    public EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    /** 我的门票列表。 */
    @GetMapping
    public ApiResponse<List<EntitlementResponse>> list() {
        return ApiResponse.ok(entitlementService.listMine(SecurityUtils.currentUserId()));
    }

    /** 我的门票详情（票务凭证）。 */
    @GetMapping("/{entitlementId}")
    public ApiResponse<EntitlementResponse> detail(@PathVariable Long entitlementId) {
        return ApiResponse.ok(
                entitlementService.getMine(SecurityUtils.currentUserId(), entitlementId));
    }
}
