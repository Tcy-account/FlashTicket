package com.flashticket.coupon.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.coupon.dto.ClaimRequest;
import com.flashticket.coupon.dto.ClaimResult;
import com.flashticket.coupon.dto.CouponAvailableResponse;
import com.flashticket.coupon.dto.UserCouponResponse;
import com.flashticket.coupon.service.CouponClaimService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端优惠券入口（M13 P0-B）：领券、领券结果查询、我的优惠券。
 *
 * <p>领券为登录用户即可用，不设独立权限点（与购票一致，属用户基础能力）。
 */
@RestController
@RequestMapping("/api/v1")
public class UserCouponController {

    private final CouponClaimService claimService;

    public UserCouponController(CouponClaimService claimService) {
        this.claimService = claimService;
    }

    /** 可领优惠券列表（公开，游客可浏览）。 */
    @GetMapping("/coupons/available")
    public ApiResponse<List<CouponAvailableResponse>> listAvailable() {
        return ApiResponse.ok(claimService.listAvailable());
    }

    /** 提交领券请求。 */
    @PostMapping("/coupons/claims")
    public ApiResponse<ClaimResult> claim(@Valid @RequestBody ClaimRequest request) {
        return ApiResponse.ok(claimService.claim(
                request.requestId(), request.templateId(), SecurityUtils.currentUserId()));
    }

    /** 查询领券结果。 */
    @GetMapping("/coupons/claims/{requestId}")
    public ApiResponse<ClaimResult> getClaimResult(@PathVariable String requestId) {
        return ApiResponse.ok(claimService.getClaimResult(requestId, SecurityUtils.currentUserId()));
    }

    /** 我的优惠券（状态过滤，可空=全部）。 */
    @GetMapping("/users/me/coupons")
    public ApiResponse<List<UserCouponResponse>> listMine(
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(claimService.listMine(SecurityUtils.currentUserId(), status));
    }
}
