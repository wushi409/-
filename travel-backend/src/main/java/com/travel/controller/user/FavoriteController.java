package com.travel.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.UserFavorite;
import com.travel.entity.TravelProduct;
import com.travel.mapper.UserFavoriteMapper;
import com.travel.mapper.TravelProductMapper;
import com.travel.service.RecommendService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@RequireRole({Constants.ROLE_USER})
@RequiredArgsConstructor
public class FavoriteController {

    private final UserFavoriteMapper favoriteMapper;
    private final TravelProductMapper productMapper;
    private final RecommendService recommendService;

    @PostMapping
    public Result<?> toggle(HttpServletRequest request, @RequestBody java.util.Map<String, Long> params) {
        Long userId = (Long) request.getAttribute("userId");
        Long productId = params.get("productId");

        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId).eq(UserFavorite::getProductId, productId);
        UserFavorite existing = favoriteMapper.selectOne(wrapper);

        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return Result.success("已取消收藏");
        } else {
            UserFavorite fav = new UserFavorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            favoriteMapper.insert(fav);
            // 记录收藏行为
            recommendService.recordBehavior(userId, productId, Constants.BEHAVIOR_FAVORITE);
            return Result.success("已收藏");
        }
    }

    @GetMapping
    public Result<?> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserFavorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>().eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreateTime));

        List<Long> productIds = favorites.stream().map(UserFavorite::getProductId).collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return Result.success(List.of());
        }
        return Result.success(productMapper.selectBatchIds(productIds));
    }

    @GetMapping("/check/{productId}")
    public Result<?> check(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = (Long) request.getAttribute("userId");
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getProductId, productId));
        return Result.success(count > 0);
    }
}
