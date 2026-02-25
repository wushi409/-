/**
 * 服务商定制处理接口：查看需求、提交方案、生成智能草稿。
 */
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
     * 服务商提交方案入口。
     * providerId 从登录态获取，避免跨账号提交到别人名下。
     */
    @PostMapping("/custom-plans")
    public Result<?> submitPlan(HttpServletRequest request, @RequestBody CustomPlan plan) {
        Long providerId = (Long) request.getAttribute("userId");
        plan.setProviderId(providerId);
        customService.submitPlan(plan);
        return Result.success("方案已提交");
    }

    /**
     * 智能草稿生成入口（服务商提案前可先用算法产出初稿）。
     * 生成失败时返回错误信息，便于前端提示人工编辑。
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
