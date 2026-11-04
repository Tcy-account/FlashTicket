package com.flashticket.purchase.enums;

/**
 * 抢票请求状态（表 ft_purchase_request.status），见 doc/抢票请求与异步排队模块.md 四。
 *
 * <p>M12 起升级为异步排队：请求先可靠受理（QUEUED），消费者领取处理租约后
 * 处理（PROCESSING），最终落在终态 SUCCEEDED（订单已创建）/ FAILED（确定业务失败）
 * / CANCELLED（用户撤销）。RECOVERING 表示处理结果暂时不确定、等待受控恢复，
 * 不会从成功/失败终态回退。
 */
public enum PurchaseStatus {
    /** 已受理，等待处理。 */
    QUEUED,
    /** 消费者已取得处理资格，正在处理。 */
    PROCESSING,
    /** 处理结果暂时不确定，等待恢复（技术故障/结果未知）。 */
    RECOVERING,
    /** 订单已创建、库存已预占（终态）。 */
    SUCCEEDED,
    /** 确定性业务失败，如售罄/限购（终态）。 */
    FAILED,
    /** 用户在允许阶段撤销请求（终态）。 */
    CANCELLED
}
