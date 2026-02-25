/**
 * 服务商订单接口：查询订单、推进履约状态、处理退款申请。
 */
package com.travel.controller.provider;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/provider/orders")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderOrderController {

    private final OrderService orderService;

    /**
     * 服务商订单列表入口。
     * 只返回当前登录服务商自己的订单，避免跨商家数据泄露。
     */
    @GetMapping
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Integer status) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(orderService.providerOrders(providerId, page, size, status));
    }

    /**
     * 服务商更新订单状态入口。
     *
     * 常见链路：
     * 1. 已付款 -> 进行中（开始履约）；
     * 2. 进行中 -> 已完成（履约完成）。
     *
     * 具体状态机校验在 OrderService#providerUpdateStatus。
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                  HttpServletRequest request,
                                  @RequestBody Map<String, Integer> params) {
        Long providerId = (Long) request.getAttribute("userId");
        orderService.providerUpdateStatus(id, providerId, params.get("status"));
        return Result.success("status updated");
    }

    /**
     * 同意退款入口。
     * 会把订单从“退款中”推进到“已退款”，并在 service 内回补库存。
     */
    @PutMapping("/{id}/refund/approve")
    public Result<?> approveRefund(@PathVariable Long id, HttpServletRequest request) {
        Long providerId = (Long) request.getAttribute("userId");
        orderService.providerHandleRefund(id, providerId, true);
        return Result.success("refund approved");
    }

    /**
     * 驳回退款入口。
     * 会把订单从“退款中”恢复为“已付款”。
     */
    @PutMapping("/{id}/refund/reject")
    public Result<?> rejectRefund(@PathVariable Long id, HttpServletRequest request) {
        Long providerId = (Long) request.getAttribute("userId");
        orderService.providerHandleRefund(id, providerId, false);
        return Result.success("refund rejected");
    }
}
