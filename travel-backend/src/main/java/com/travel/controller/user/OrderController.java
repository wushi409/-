/**
 * 游客订单接口：下单、支付、取消、退款申请。
 */
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

@RestController
@RequestMapping("/api/orders")
@RequireRole({Constants.ROLE_USER})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RecommendService recommendService;

    /**
     * 下单入口（用户链路）。
     *
     * 流程：
     * 1. 从登录上下文拿 userId；
     * 2. 调用 service 创建订单（初始状态：待付款）；
     * 3. 记录一次购买行为，用于后续推荐。
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

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    /**
     * 支付入口（演示版）。
     * 当前直接把订单状态改成“已付款”，库存扣减在 service 内联动完成。
     */
    @PutMapping("/{id}/pay")
    public Result<?> pay(@PathVariable Long id) {
        orderService.updateStatus(id, Constants.ORDER_PAID);
        return Result.success("payment success");
    }

    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.cancelOrder(id, userId);
        return Result.success("cancel success");
    }

    /**
     * 退款申请入口（用户主动发起）。
     * 仅把订单推进到“退款中”，最终是否退款由服务商审核决定。
     */
    @PutMapping("/{id}/refund")
    public Result<?> requestRefund(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.requestRefund(id, userId);
        return Result.success("refund requested");
    }
}
