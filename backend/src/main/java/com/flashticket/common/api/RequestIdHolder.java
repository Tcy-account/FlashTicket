package com.flashticket.common.api;

import org.slf4j.MDC;

/**
 * 请求级标识持有者：从 MDC 读取当前请求的 requestId / traceId。
 *
 * <p>概念区分（见跨域审查 6.4）：requestId 是本次请求业务号，可用于幂等追踪与对账；
 * traceId 是链路追踪 ID，仅用于可观测查询，禁止当幂等键（调用重试会产生新 Trace）。
 */
public final class RequestIdHolder {

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_TRACE_ID = "traceId";

    private RequestIdHolder() {
    }

    public static String currentRequestId() {
        return MDC.get(MDC_REQUEST_ID);
    }

    public static String currentTraceId() {
        return MDC.get(MDC_TRACE_ID);
    }
}
