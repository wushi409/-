package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.TravelOrder;
import com.travel.service.OrderService;
import com.travel.service.RecommendService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 类说明：OrderController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/orders")
@RequireRole({Constants.ROLE_USER})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RecommendService recommendService;

    /**
     * 方法说明：create
     * 1. 负责处理 create 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody TravelOrder order) {
        Long userId = (Long) request.getAttribute("userId");
        order.setUserId(userId);
        orderService.createOrder(order);
        if (order.getProductId() != null) {
            recommendService.recordBehavior(userId, order.getProductId(), Constants.BEHAVIOR_PURCHASE);
        }
        return Result.success("order created");
    }

    @GetMapping
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Integer status) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.userOrders(userId, page, size, status));
    }

    /**
     * 方法说明：detail
     * 1. 负责处理 detail 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    /**
     * 方法说明：pay
     * 1. 负责处理 pay 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/{id}/pay")
    public Result<?> pay(@PathVariable Long id) {
        orderService.updateStatus(id, Constants.ORDER_PAID);
        return Result.success("payment success");
    }

    /**
     * 方法说明：cancel
     * 1. 负责处理 cancel 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.cancelOrder(id, userId);
        return Result.success("cancel success");
    }

    /**
     * 方法说明：requestRefund
     * 1. 负责处理 requestRefund 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/{id}/refund")
    public Result<?> requestRefund(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.requestRefund(id, userId);
        return Result.success("refund requested");
    }
}
