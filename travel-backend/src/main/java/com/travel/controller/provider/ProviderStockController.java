package com.travel.controller.provider;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.service.DailyStockService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 类说明：ProviderStockController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/provider/stocks")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderStockController {

    private final DailyStockService dailyStockService;

    @GetMapping
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(required = false) Long productId,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(dailyStockService.listProviderStocks(providerId, productId, startDate, endDate, page, size));
    }

    @PostMapping("/batch")
    public Result<?> batchUpsert(HttpServletRequest request,
                                 @RequestBody DailyStockService.BatchStockRequest payload) {
        Long providerId = (Long) request.getAttribute("userId");
        dailyStockService.batchUpsert(providerId, payload);
        return Result.success("daily stock updated");
    }

    @GetMapping("/warnings")
    public Result<?> warnings(HttpServletRequest request,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size) {
        Long providerId = (Long) request.getAttribute("userId");
        return Result.success(dailyStockService.listWarnings(providerId, page, size));
    }
}

