package com.travel.controller.admin;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.service.OrderService;
import com.travel.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 类说明：AdminStatController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole({Constants.ROLE_ADMIN})
@RequiredArgsConstructor
public class AdminStatController {

    private final StatService statService;
    private final OrderService orderService;

    /**
     * 方法说明：overview
     * 1. 负责处理 overview 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/stats/overview")
    public Result<?> overview() {
        return Result.success(statService.overview());
    }

    /**
     * 方法说明：orderTrend
     * 1. 负责处理 orderTrend 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/stats/orders")
    public Result<?> orderTrend(@RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statService.orderTrend(days));
    }

    /**
     * 方法说明：hotDestinations
     * 1. 负责处理 hotDestinations 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/stats/hot-destinations")
    public Result<?> hotDestinations(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(statService.hotDestinations(limit));
    }

    /**
     * 方法说明：userPreferences
     * 1. 负责处理 userPreferences 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/stats/user-preferences")
    public Result<?> userPreferences(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(statService.userPreferences(limit));
    }

    @GetMapping("/orders")
    public Result<?> listOrders(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(required = false) String keyword) {
        return Result.success(orderService.adminOrders(page, size, status, keyword));
    }

    @PutMapping("/orders/{id}/status")
    public Result<?> updateOrderStatus(@PathVariable Long id,
                                       @RequestBody java.util.Map<String, Integer> params) {
        orderService.updateStatus(id, params.get("status"));
        return Result.success("order status updated");
    }
}
