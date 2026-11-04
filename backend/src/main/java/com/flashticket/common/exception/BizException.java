package com.flashticket.common.exception;

import com.flashticket.common.api.ErrorCode;

/**
 * 业务异常：携带通用或领域错误码，由全局异常处理器统一转换为响应信封。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 便捷构造：使用错误码自带的默认 message。 */
    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
