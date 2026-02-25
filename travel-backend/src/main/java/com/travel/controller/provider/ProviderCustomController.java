package com.travel.controller.provider;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.CustomPlan;
import com.travel.service.CustomService;
import com.travel.service.TripPlannerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 类说明：ProviderCustomController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/provider")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderCustomController {

    private final CustomService customService;
    private final TripPlannerService tripPlannerService;

    @GetMapping("/custom-requests")
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(customService.providerRequests(providerId, page, size));
    }

    /**
     * 方法说明：submitPlan
     * 1. 负责处理 submitPlan 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/custom-plans")
    public Result<?> submitPlan(HttpServletRequest request, @RequestBody CustomPlan plan) {
        Long providerId = (Long) request.getAttribute("userId");
        plan.setProviderId(providerId);
        customService.submitPlan(plan);
        return Result.success("方案已提交");
    }

    /**
     * 智能生成方案 - 基于约束的行程自动生成算法
     * 根据用户的定制需求（预算、天数、目的地、兴趣标签）自动生成旅游方案
     */
    @PostMapping("/custom-requests/{id}/generate")
    public Result<?> generatePlan(@PathVariable Long id) {
        try {
            TripPlannerService.GeneratedPlan plan = tripPlannerService.generatePlan(id);
            return Result.success(plan);
        } catch (Exception e) {
            return Result.error("生成方案失败：" + e.getMessage());
        }
    }
}
