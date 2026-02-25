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

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

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

