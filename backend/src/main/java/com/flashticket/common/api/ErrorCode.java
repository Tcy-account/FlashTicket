package com.flashticket.common.api;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码（契约见 doc/M00-工程初始化.md 3.2）。
 *
 * <p>命名约定：通用码用简短语义；领域业务码用 {@code {域}_{含义}} 全大写，
 * 如 AUTH_USERNAME_TAKEN / INVENTORY_SOLD_OUT / ORDER_ALREADY_PAID。
 * 枚举名即业务码，保证 code 与枚举一一对应、不出现同义多码。
 *
 * <p>每个错误码携带默认简体中文 message，供 {@link BizException} 便捷构造与
 * {@link ApiResponse#ok()} 等场景复用；必要时仍可显式覆盖 message。
 */
public enum ErrorCode {

    // ===== 通用 =====
    SUCCESS(HttpStatus.OK, "操作成功"),
    PARAM_ERROR(HttpStatus.BAD_REQUEST, "请求参数校验失败"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "未认证或凭据无效"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "无权访问该资源"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "请求的资源不存在"),
    CONFLICT(HttpStatus.CONFLICT, "资源状态冲突"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误，请稍后重试"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "依赖服务不可用"),

    // ===== identity 域（M01 注册与邮箱验证） =====
    AUTH_USERNAME_TAKEN(HttpStatus.CONFLICT, "用户名已被占用"),
    AUTH_EMAIL_TAKEN(HttpStatus.CONFLICT, "邮箱已被注册"),
    AUTH_VERIFY_CODE_INVALID(HttpStatus.BAD_REQUEST, "验证码错误或已过期"),
    AUTH_VERIFY_CODE_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "验证码尝试次数过多，请重新获取"),
    AUTH_VERIFY_CODE_SENT_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试"),
    AUTH_EMAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "验证码发送失败，请稍后重试"),

    // ===== identity 域（M02 登录认证与个人中心） =====
    AUTH_BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "账号已被禁用，请联系客服"),
    AUTH_ACCOUNT_LOCKED(HttpStatus.LOCKED, "登录尝试过于频繁，账号已临时锁定，请稍后再试"),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录"),
    AUTH_OLD_PASSWORD_WRONG(HttpStatus.BAD_REQUEST, "当前密码不正确"),
    AUTH_PASSWORD_SAME(HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同"),
    AUTH_SESSION_LIMIT(HttpStatus.FORBIDDEN, "登录设备数量已达上限，请先在其他设备下线"),

    // ===== identity 域（M03 后台账号与 RBAC） =====
    AUTH_ADMIN_FORBIDDEN(HttpStatus.FORBIDDEN, "该账号无后台访问权限"),
    RBAC_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "角色不存在"),
    RBAC_ROLE_NOT_GRANTABLE(HttpStatus.FORBIDDEN, "无权授予该角色"),
    RBAC_SELF_ESCALATION(HttpStatus.FORBIDDEN, "不能提升自己的权限"),
    RBAC_LAST_SUPER_ADMIN(HttpStatus.CONFLICT, "系统必须保留至少一个有效的超级管理员"),

    // ===== event 域（M04 演出分类与场馆管理） =====
    EVENT_CATEGORY_CODE_TAKEN(HttpStatus.CONFLICT, "分类码已被占用"),
    EVENT_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "分类不存在"),
    EVENT_VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "场馆不存在"),
    EVENT_VENUE_CAPACITY_INVALID(HttpStatus.BAD_REQUEST, "场馆最大容量必须大于 0"),

    // ===== event 域（M05 演出活动与场次管理） =====
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "活动不存在"),
    EVENT_CATEGORY_DISABLED(HttpStatus.BAD_REQUEST, "所选分类已停用"),
    EVENT_NOT_CANCELLABLE(HttpStatus.CONFLICT, "活动当前状态不可取消"),
    EVENT_VERSION_CONFLICT(HttpStatus.CONFLICT, "活动已被其他操作修改，请刷新后重试"),
    EVENT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "场次不存在"),
    EVENT_SESSION_INVALID_TIME(HttpStatus.BAD_REQUEST, "演出结束时间必须晚于开始时间"),
    EVENT_SESSION_ENTRY_INVALID(HttpStatus.BAD_REQUEST, "入场时间必须早于演出开始时间"),
    EVENT_SESSION_CAPACITY_EXCEED(HttpStatus.BAD_REQUEST, "场次可售容量不能超过场馆核定容量"),
    EVENT_SESSION_TIME_CONFLICT(HttpStatus.CONFLICT, "该场馆该时段已有场次，请调整时间"),
    EVENT_SESSION_ILLEGAL_TRANSITION(HttpStatus.CONFLICT, "场次当前状态不允许该操作"),
    EVENT_SESSION_VERSION_CONFLICT(HttpStatus.CONFLICT, "场次已被其他操作修改，请刷新后重试"),

    // ===== event 域（M06 票档、售票计划与审核发布） =====
    EVENT_TIER_NOT_FOUND(HttpStatus.NOT_FOUND, "票档不存在"),
    EVENT_TIER_NAME_TAKEN(HttpStatus.CONFLICT, "同场次下票档名称已存在"),
    EVENT_TIER_CAPACITY_EXCEED(HttpStatus.BAD_REQUEST, "票档总库存不能超过场次可售容量"),
    EVENT_TIER_INVALID_LIMIT(HttpStatus.BAD_REQUEST, "每单最大购买数不能小于最小购买数"),
    EVENT_TIER_VERSION_CONFLICT(HttpStatus.CONFLICT, "票档已被其他操作修改，请刷新后重试"),
    EVENT_SALE_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "售票计划不存在"),
    EVENT_SALE_PLAN_INVALID_TIME(HttpStatus.BAD_REQUEST, "售票结束时间必须晚于开售时间"),
    EVENT_SALE_PLAN_ILLEGAL_TRANSITION(HttpStatus.CONFLICT, "售票计划当前状态不允许该操作"),
    EVENT_SUBMIT_NOT_READY(HttpStatus.CONFLICT, "活动配置不完整：需至少一个有效场次，且每个场次至少一个已配置售票计划的票档"),
    EVENT_SUBMIT_ILLEGAL_STATE(HttpStatus.CONFLICT, "活动当前状态不可提交审核"),
    EVENT_REVIEW_SELF_REVIEW(HttpStatus.FORBIDDEN, "不能审核自己提交的活动"),
    EVENT_REVIEW_ILLEGAL_STATE(HttpStatus.CONFLICT, "活动当前状态不可审核"),
    EVENT_REVIEW_COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "审核驳回必须填写原因"),
    EVENT_PUBLISH_NOT_READY(HttpStatus.CONFLICT, "活动发布信息不完整：封面、详情、主办方与观演须知均为必填"),
    EVENT_PUBLISH_ILLEGAL_STATE(HttpStatus.CONFLICT, "活动当前状态不可发布"),
    EVENT_PUBLISH_FINGERPRINT_MISMATCH(HttpStatus.CONFLICT, "审核配置已被修改，请重新提交审核"),
    EVENT_CONFIG_FROZEN(HttpStatus.CONFLICT, "活动配置已进入审核或发布流程，当前不可修改"),

    // ===== inventory 域（M07 MySQL 库存总账与用户限购） =====
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "库存账本不存在，票档尚未初始化库存"),
    INVENTORY_SOLD_OUT(HttpStatus.CONFLICT, "可售库存不足"),
    INVENTORY_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "购买数量非法"),
    INVENTORY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "超出该票档每人限购数量"),
    INVENTORY_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "库存预占记录不存在"),
    INVENTORY_RESERVATION_ILLEGAL_TRANSITION(HttpStatus.CONFLICT, "预占当前状态不允许该操作"),

    // ===== purchase 域（M08 同步抢票） =====
    PURCHASE_EVENT_NOT_PUBLISHED(HttpStatus.CONFLICT, "活动未发布，暂不可购票"),
    PURCHASE_SESSION_NOT_SALE(HttpStatus.CONFLICT, "该场次当前不可售票"),
    PURCHASE_SALE_PAUSED(HttpStatus.CONFLICT, "该票档售票已暂停"),
    PURCHASE_WINDOW_NOT_OPEN(HttpStatus.CONFLICT, "不在售票时间窗口内"),

    // ===== purchase 域（M12 异步抢票与排队） =====
    PURCHASE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "抢票请求不存在"),
    PURCHASE_NOT_CANCELLABLE(HttpStatus.CONFLICT, "抢票请求当前状态不可撤销"),

    // ===== order 域（M08 订单创建与查询） =====
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "订单不存在"),
    ORDER_NOT_OWNED(HttpStatus.FORBIDDEN, "无权访问该订单"),

    // ===== order 域（M09 订单取消与超时关单） =====
    ORDER_NOT_CANCELLABLE(HttpStatus.CONFLICT, "订单当前状态不可取消"),

    // ===== payment 域（M10 模拟支付与票务权益） =====
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "支付单不存在"),
    PAYMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "无权访问该支付单"),
    PAYMENT_ORDER_NOT_PAYABLE(HttpStatus.CONFLICT, "订单当前状态不可支付"),
    PAYMENT_ORDER_EXPIRED(HttpStatus.CONFLICT, "订单已超时，不可支付"),
    PAYMENT_ORDER_CLOSED(HttpStatus.CONFLICT, "订单已关闭，支付失败"),

    // ===== entitlement 域（M10 票务权益） =====
    ENTITLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "票务权益不存在"),
    ENTITLEMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "无权访问该票务权益"),

    // ===== refund 域（M11 全额退款与权益撤销） =====
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "退款单不存在"),
    REFUND_NOT_OWNED(HttpStatus.FORBIDDEN, "无权访问该退款单"),
    REFUND_ORDER_NOT_REFUNDABLE(HttpStatus.CONFLICT, "订单当前状态不可退款"),
    REFUND_ALREADY_EXISTS(HttpStatus.CONFLICT, "该订单已存在退款申请"),
    REFUND_SELF_REVIEW(HttpStatus.FORBIDDEN, "不能审核自己提交的退款申请"),
    REFUND_ILLEGAL_TRANSITION(HttpStatus.CONFLICT, "退款当前状态不允许该操作"),
    REFUND_REVIEW_COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "审核拒绝必须填写原因"),
    REFUND_NOT_CANCELLABLE(HttpStatus.CONFLICT, "退款当前状态不可撤销"),

    // ===== coupon 域（M13 优惠券与营销活动） =====
    CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND, "营销活动不存在"),
    CAMPAIGN_TIME_INVALID(HttpStatus.BAD_REQUEST, "活动结束时间必须晚于开始时间"),
    CAMPAIGN_ILLEGAL_STATE(HttpStatus.CONFLICT, "营销活动当前状态不允许该操作"),
    CAMPAIGN_SELF_REVIEW(HttpStatus.FORBIDDEN, "不能审核自己提交的营销活动"),
    CAMPAIGN_REVIEW_COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "审核驳回必须填写原因"),
    COUPON_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "优惠券模板不存在"),
    COUPON_TEMPLATE_TIME_INVALID(HttpStatus.BAD_REQUEST, "模板领取或有效期配置非法"),
    COUPON_TEMPLATE_FROZEN(HttpStatus.CONFLICT, "模板已发布，优惠规则不可修改"),
    COUPON_TEMPLATE_ILLEGAL_STATE(HttpStatus.CONFLICT, "优惠券模板当前状态不允许该操作"),
    COUPON_NOT_CLAIMABLE(HttpStatus.CONFLICT, "优惠券当前不可领取"),
    COUPON_CLAIM_WINDOW_NOT_OPEN(HttpStatus.CONFLICT, "不在领取时间窗口内"),
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "优惠券已领完"),
    COUPON_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "超出该优惠券每人限领数量"),
    COUPON_CLAIM_ALREADY_EXISTS(HttpStatus.CONFLICT, "该领券请求已处理"),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "用户优惠券不存在"),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "无权使用该优惠券"),
    COUPON_NOT_APPLICABLE(HttpStatus.CONFLICT, "优惠券不适用当前票档或活动"),
    COUPON_NOT_AVAILABLE(HttpStatus.CONFLICT, "优惠券当前不可用"),
    COUPON_THRESHOLD_NOT_MET(HttpStatus.CONFLICT, "未达到优惠券使用门槛"),

    // ===== notification 域（M14 系统通知与 SSE） =====
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "通知不存在"),
    NOTIFICATION_NOT_OWNED(HttpStatus.FORBIDDEN, "无权访问该通知"),
    NOTIFICATION_STREAM_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "实时通知凭证无效或已过期"),

    // ===== risk 域（M15 风控与限流） =====
    RISK_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试"),
    RISK_RESTRICTED(HttpStatus.FORBIDDEN, "账号部分业务受限，请稍后再试"),
    RISK_RESTRICTION_NOT_FOUND(HttpStatus.NOT_FOUND, "风控限制不存在"),
    RISK_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "风控规则不存在"),
    RISK_RULE_CODE_TAKEN(HttpStatus.CONFLICT, "风控规则码已被占用"),

    // ===== analytics / reconciliation 域（M16 统计与对账） =====
    ANALYTICS_RANGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "统计时间范围过大，请缩小查询窗口"),
    RECONCILE_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "对账任务不存在"),
    RECONCILE_EXCEPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "对账异常工单不存在"),
    RECONCILE_ILLEGAL_TRANSITION(HttpStatus.CONFLICT, "对账工单状态流转不合法"),
    COMPENSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "补偿任务不存在"),
    COMPENSATION_ILLEGAL_TRANSITION(HttpStatus.CONFLICT, "补偿任务状态流转不合法");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /** 业务码，即枚举名。 */
    public String getCode() {
        return name();
    }

    /** 默认简体中文提示。 */
    public String getMessage() {
        return message;
    }
}
