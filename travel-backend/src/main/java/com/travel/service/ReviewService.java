package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Constants;
import com.travel.common.PageResult;
import com.travel.common.exception.BusinessException;
import com.travel.entity.Review;
import com.travel.entity.TravelOrder;
import com.travel.entity.User;
import com.travel.entity.UserBehavior;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.TravelOrderMapper;
import com.travel.mapper.UserBehaviorMapper;
import com.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final TravelOrderMapper orderMapper;
    private final UserMapper userMapper;
    private final UserBehaviorMapper behaviorMapper;

    public void createReview(Long userId, Review review) {
        if (review == null) {
            throw new BusinessException("invalid review payload");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new BusinessException("rating must be between 1 and 5");
        }
        if (review.getOrderId() == null) {
            throw new BusinessException("orderId is required");
        }

        TravelOrder order = orderMapper.selectById(review.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException("order not found");
        }
        if (order.getStatus() == null || order.getStatus() != Constants.ORDER_COMPLETED) {
            throw new BusinessException("only completed orders can be reviewed");
        }
        if (order.getProductId() == null) {
            throw new BusinessException("order product not found");
        }

        Long reviewedCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId()));
        if (reviewedCount != null && reviewedCount > 0) {
            throw new BusinessException("order already reviewed");
        }

        review.setUserId(userId);
        review.setProductId(order.getProductId());
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);

        // 同步评分行为到推荐系统行为表
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setProductId(order.getProductId());
        behavior.setBehaviorType(Constants.BEHAVIOR_RATE);
        behavior.setScore(BigDecimal.valueOf(review.getRating()));
        behaviorMapper.insert(behavior);
    }

    public PageResult<Review> listByProduct(Long productId, Integer page, Integer size) {
        if (productId == null) {
            throw new BusinessException("productId is required");
        }
        Page<Review> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getProductId, productId).orderByDesc(Review::getCreateTime);
        Page<Review> result = reviewMapper.selectPage(pageParam, wrapper);

        for (Review review : result.getRecords()) {
            User user = userMapper.selectById(review.getUserId());
            if (user != null) {
                review.setUserName(user.getNickname());
                review.setUserAvatar(user.getAvatar());
            }
        }

        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
}

