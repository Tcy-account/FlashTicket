package com.flashticket.testfixture;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅测试环境加载的异常映射验证端点，生产代码不依赖此文件。
 */
@RestController
@RequestMapping("/test")
@Profile("test")
public class TestExceptionController {

    @GetMapping("/biz-error")
    public ApiResponse<Void> bizError() {
        throw new BizException(ErrorCode.CONFLICT, "模拟业务冲突");
    }
}
