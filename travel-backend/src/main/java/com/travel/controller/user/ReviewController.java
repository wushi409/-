package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.Review;
import com.travel.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类说明：ReviewController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 方法说明：create
     * 1. 负责处理 create 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping
    @RequireRole({Constants.ROLE_USER})
    public Result<?> create(HttpServletRequest request, @RequestBody Review review) {
        Long userId = (Long) request.getAttribute("userId");
        reviewService.createReview(userId, review);
        return Result.success("review created");
    }

    @GetMapping("/product/{productId}")
    public Result<?> listByProduct(@PathVariable Long productId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(reviewService.listByProduct(productId, page, size));
    }
}

