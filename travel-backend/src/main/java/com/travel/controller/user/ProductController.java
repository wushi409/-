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

