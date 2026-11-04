package com.flashticket.common.exception;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：所有异常统一转换为响应信封（契约见 doc/M00-工程初始化.md 3.4）。
 *
 * <p>安全约束：500 响应不返回堆栈与内部结构；日志记录完整异常，响应体脱敏。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 参数校验失败（@Valid 请求体）：取第一条校验信息，400 PARAM_ERROR。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("请求参数校验失败");
        return build(ErrorCode.PARAM_ERROR, message);
    }

    /** 参数校验失败（@Validated 方法参数级）。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("请求参数校验失败");
        return build(ErrorCode.PARAM_ERROR, message);
    }

    /** 请求体不可读（JSON 语法错误、类型不匹配）。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.PARAM_ERROR, "请求体格式不正确");
    }

    /** 业务异常：错误码透传，HTTP 状态由错误码决定。 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex) {
        return build(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return build(ErrorCode.UNAUTHORIZED, "未认证或凭据无效");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.FORBIDDEN, "无权访问该资源");
    }

    /** 静态资源/路径不存在。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return build(ErrorCode.NOT_FOUND, "请求的资源不存在");
    }

    /** 数据访问异常：日志记堆栈，响应脱敏。 */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        log.error("数据访问异常", ex);
        return build(ErrorCode.INTERNAL_ERROR, "系统内部错误，请稍后重试");
    }

    /** 兜底异常：日志记堆栈，响应脱敏，不向前端泄露任何内部细节。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("未捕获系统异常", ex);
        return build(ErrorCode.INTERNAL_ERROR, "系统内部错误，请稍后重试");
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorCode, message));
    }
}
