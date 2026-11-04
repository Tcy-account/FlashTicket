package com.flashticket.purchase.service;

/**
 * 抢票处理租约失效异常（M12）。
 *
 * <p>消费者在提交建单事务时，若发现租约已被恢复扫描器收回或被其他消费者抢占
 * （markSucceeded 条件更新返回 0），抛出本异常使当前事务整体回滚，请求交由
 * 持有新租约的处理者继续推进。这不是业务失败，不能标记 FAILED。
 */
public class LeaseLostException extends RuntimeException {

    public LeaseLostException() {
        super("purchase lease lost");
    }
}
