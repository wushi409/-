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

@RestController
@RequestMapping("/api/provider/stats")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderStatController {

    private final ProviderStatService providerStatService;

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

