package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.service.ProductService;
import com.travel.service.RecommendService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 类说明：ProductController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final RecommendService recommendService;

    @GetMapping("/products")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long destinationId,
                          @RequestParam(required = false) Integer productType,
                          @RequestParam(required = false) BigDecimal priceMin,
                          @RequestParam(required = false) BigDecimal priceMax,
                          @RequestParam(required = false) String sortBy) {
        return Result.success(productService.list(page, size, keyword, destinationId, productType, priceMin, priceMax, sortBy));
    }

    /**
     * 方法说明：detail
     * 1. 负责处理 detail 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/products/{id}")
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            recommendService.recordBehavior(userId, id, Constants.BEHAVIOR_VIEW);
        }
        return Result.success(productService.getById(id));
    }

    @GetMapping("/recommend")
    public Result<?> recommend(HttpServletRequest request,
                               @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.success(recommendService.recommend(0L, limit));
        }
        return Result.success(recommendService.recommend(userId, limit));
    }
}

