package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Constants;
import com.travel.entity.Destination;
import com.travel.entity.TravelOrder;
import com.travel.entity.TravelProduct;
import com.travel.entity.User;
import com.travel.entity.UserBehavior;
import com.travel.mapper.DestinationMapper;
import com.travel.mapper.TravelOrderMapper;
import com.travel.mapper.TravelProductMapper;
import com.travel.mapper.UserBehaviorMapper;
import com.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatService {

    private static final String TEST_TAG_API = "接口测试";

    private final UserMapper userMapper;
    private final TravelOrderMapper orderMapper;
    private final TravelProductMapper productMapper;
    private final DestinationMapper destinationMapper;
    private final UserBehaviorMapper behaviorMapper;

    public Map<String, Object> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, 1)));
        data.put("providerCount", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, 2)));
        data.put("orderCount", orderMapper.selectCount(null));
        data.put("productCount", productMapper.selectCount(
                new LambdaQueryWrapper<TravelProduct>().eq(TravelProduct::getStatus, 1)));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        data.put("todayOrders", orderMapper.selectCount(
                new LambdaQueryWrapper<TravelOrder>().ge(TravelOrder::getCreateTime, todayStart)));

        return data;
    }

    public List<Map<String, Object>> orderTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            Long count = orderMapper.selectCount(
                    new LambdaQueryWrapper<TravelOrder>()
                            .ge(TravelOrder::getCreateTime, start)
                            .lt(TravelOrder::getCreateTime, end));

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> hotDestinations(int limit) {
        List<Destination> destinations = destinationMapper.selectList(
                new LambdaQueryWrapper<Destination>()
                        .eq(Destination::getStatus, 1)
                        .orderByDesc(Destination::getHotScore)
                        .last("LIMIT " + limit));

        return destinations.stream().map(d -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", d.getName());
            item.put("hotScore", d.getHotScore());
            Long productCount = productMapper.selectCount(
                    new LambdaQueryWrapper<TravelProduct>()
                            .eq(TravelProduct::getDestinationId, d.getId())
                            .eq(TravelProduct::getStatus, 1));
            item.put("productCount", productCount);
            return item;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> userPreferences(Integer limit) {
        int topN = (limit == null || limit <= 0) ? 10 : limit;
        List<UserBehavior> behaviors = behaviorMapper.selectList(null);
        if (behaviors.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = new HashSet<>();
        for (UserBehavior behavior : behaviors) {
            if (behavior.getProductId() != null) {
                productIds.add(behavior.getProductId());
            }
        }
        if (productIds.isEmpty()) {
            return List.of();
        }

        Map<Long, TravelProduct> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(TravelProduct::getId, p -> p));

        Map<String, Double> tagScoreMap = new HashMap<>();
        for (UserBehavior behavior : behaviors) {
            TravelProduct product = productMap.get(behavior.getProductId());
            if (product == null || product.getTags() == null || product.getTags().isBlank()) {
                continue;
            }

            double weight;
            if (behavior.getBehaviorType() == Constants.BEHAVIOR_FAVORITE) {
                weight = Constants.WEIGHT_FAVORITE;
            } else if (behavior.getBehaviorType() == Constants.BEHAVIOR_PURCHASE) {
                weight = Constants.WEIGHT_PURCHASE;
            } else if (behavior.getBehaviorType() == Constants.BEHAVIOR_RATE) {
                weight = behavior.getScore() != null ? behavior.getScore().doubleValue() : 3.0;
            } else {
                weight = Constants.WEIGHT_VIEW;
            }

            String[] tags = product.getTags().split(",");
            for (String rawTag : tags) {
                String tag = rawTag.trim();
                if (tag.isEmpty() || TEST_TAG_API.equals(tag)) {
                    continue;
                }
                tagScoreMap.merge(tag, weight, Double::sum);
            }
        }

        return tagScoreMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("tag", entry.getKey());
                    item.put("score", Math.round(entry.getValue() * 100.0) / 100.0);
                    return item;
                })
                .collect(Collectors.toList());
    }
}
