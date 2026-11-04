package com.flashticket.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * 统一 API 响应信封（契约见 doc/M00-工程初始化.md 3.1）。
 *
 * <p>HTTP 状态码与业务码分离：HTTP 码管传输语义（2xx/4xx/5xx），
 * {@code code} 管业务语义；前端以 {@code code == "SUCCESS"} 判定成功。
 *
 * @param <T> 业务数据类型
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String requestId,
        String traceId,
        Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return of(ErrorCode.SUCCESS, ErrorCode.SUCCESS.getMessage(), data);
    }

    public static ApiResponse<Void> ok() {
        return of(ErrorCode.SUCCESS, ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return of(errorCode, message, null);
    }

    public static <T> ApiResponse<T> of(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(
                errorCode.getCode(),
                message,
                data,
                RequestIdHolder.currentRequestId(),
                RequestIdHolder.currentTraceId(),
                Instant.now());
    }
}
