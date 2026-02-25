package com.travel.controller.admin;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.service.OrderService;
import com.travel.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequireRole({Constants.ROLE_ADMIN})
@RequiredArgsConstructor
public class AdminStatController {

    private final StatService statService;
    private final OrderService orderService;

    @GetMapping("/stats/overview")
    public Result<?> overview() {
        return Result.success(statService.overview());
    }

    @GetMapping("/stats/orders")
    public Result<?> orderTrend(@RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statService.orderTrend(days));
    }

    @GetMapping("/stats/hot-destinations")
    public Result<?> hotDestinations(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(statService.hotDestinations(limit));
    }

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
