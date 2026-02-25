package com.travel.controller.provider;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.service.ProviderStatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类说明：ProviderStatController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/provider/stats")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderStatController {

    private final ProviderStatService providerStatService;

    /**
     * 方法说明：overview
     * 1. 负责处理 overview 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/overview")
    public Result<?> overview(HttpServletRequest request) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(providerStatService.overview(providerId));
    }

    @GetMapping("/orders")
    public Result<?> orderTrend(HttpServletRequest request,
                                @RequestParam(defaultValue = "30") Integer days) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(providerStatService.orderTrend(providerId, days));
    }

    @GetMapping("/hot-products")
    public Result<?> hotProducts(HttpServletRequest request,
                                 @RequestParam(defaultValue = "10") Integer limit) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(providerStatService.hotProducts(providerId, limit));
    }
}

