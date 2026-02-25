package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.CustomRequest;
import com.travel.service.CustomService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 类说明：CustomRequestController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomRequestController {

    private final CustomService customService;

    /**
     * 方法说明：submit
     * 1. 负责处理 submit 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/custom-requests")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> submit(HttpServletRequest request, @RequestBody CustomRequest customRequest) {
        Long userId = (Long) request.getAttribute("userId");
        customRequest.setUserId(userId);
        customService.submitRequest(customRequest);
        return Result.success("request submitted");
    }

    @GetMapping("/custom-requests")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> myRequests(HttpServletRequest request,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(customService.userRequests(userId, page, size));
    }

    /**
     * 方法说明：detail
     * 1. 负责处理 detail 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/custom-requests/{id}")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(customService.getRequestByIdForUser(id, userId));
    }

    /**
     * 方法说明：getPlan
     * 1. 负责处理 getPlan 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/custom-requests/{id}/plan")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> getPlan(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(customService.getPlanByRequestIdForUser(id, userId));
    }

    /**
     * 方法说明：acceptPlan
     * 1. 负责处理 acceptPlan 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/custom-plans/{id}/accept")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> acceptPlan(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        customService.acceptPlan(id, userId);
        return Result.success("plan accepted");
    }

    /**
     * 方法说明：rejectPlan
     * 1. 负责处理 rejectPlan 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/custom-plans/{id}/reject")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> rejectPlan(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        customService.rejectPlan(id, userId);
        return Result.success("plan rejected");
    }
}
