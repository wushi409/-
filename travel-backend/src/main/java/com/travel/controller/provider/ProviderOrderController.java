package com.travel.controller.provider;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 类说明：ProviderOrderController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/provider/orders")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderOrderController {

    private final OrderService orderService;

    @GetMapping
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Integer status) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(orderService.providerOrders(providerId, page, size, status));
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                  HttpServletRequest request,
                                  @RequestBody Map<String, Integer> params) {
        Long providerId = (Long) request.getAttribute("userId");
        orderService.providerUpdateStatus(id, providerId, params.get("status"));
        return Result.success("status updated");
    }

    /**
     * 方法说明：approveRefund
     * 1. 负责处理 approveRefund 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/{id}/refund/approve")
    public Result<?> approveRefund(@PathVariable Long id, HttpServletRequest request) {
        Long providerId = (Long) request.getAttribute("userId");
        orderService.providerHandleRefund(id, providerId, true);
        return Result.success("refund approved");
    }

    /**
     * 方法说明：rejectRefund
     * 1. 负责处理 rejectRefund 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/{id}/refund/reject")
    public Result<?> rejectRefund(@PathVariable Long id, HttpServletRequest request) {
        Long providerId = (Long) request.getAttribute("userId");
        orderService.providerHandleRefund(id, providerId, false);
        return Result.success("refund rejected");
    }
}
