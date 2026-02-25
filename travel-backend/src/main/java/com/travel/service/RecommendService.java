package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Constants;
import com.travel.entity.TravelProduct;
import com.travel.entity.UserBehavior;
import com.travel.mapper.TravelProductMapper;
import com.travel.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final UserBehaviorMapper behaviorMapper;
    private final TravelProductMapper productMapper;

    // 缓存产品相似度矩阵：itemA -> (itemB -> similarity)
    private Map<Long, Map<Long, Double>> itemSimilarityMatrix = new ConcurrentHashMap<>();

    /**
     * 为指定用户生成 TopN 推荐列表。
     * 若用户历史行为为空（冷启动）或计算结果为空，则降级返回热门产品。
     */
    public List<TravelProduct> recommend(Long userId, int topN) {
        // 如果相似度矩阵为空，先计算
        if (itemSimilarityMatrix.isEmpty()) {
            refreshSimilarityMatrix();
        }

        // 获取用户的行为数据
        List<UserBehavior> userBehaviors = behaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>().eq(UserBehavior::getUserId, userId));

        // 冷启动：如果用户没有行为数据，返回热门产品
        if (userBehaviors.isEmpty()) {
            return getHotProducts(topN);
        }

        // 构建当前用户对各产品的“偏好强度”映射。
        Map<Long, Double> userRatings = buildUserRatingMap(userBehaviors);

        // 计算候选产品分数：score(candidate) += similarity(item, candidate) * rating(item)
        Map<Long, Double> recommendScores = new HashMap<>();
        for (Map.Entry<Long, Double> entry : userRatings.entrySet()) {
            Long itemId = entry.getKey();
            Double rating = entry.getValue();

            Map<Long, Double> similarities = itemSimilarityMatrix.getOrDefault(itemId, Collections.emptyMap());
            for (Map.Entry<Long, Double> simEntry : similarities.entrySet()) {
                Long candidateId = simEntry.getKey();
                Double similarity = simEntry.getValue();

                // 排除用户已交互的产品
                if (userRatings.containsKey(candidateId)) continue;

                recommendScores.merge(candidateId, similarity * rating, Double::sum);
            }
        }

        // 排序取TopN
        List<Long> recommendIds = recommendScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (recommendIds.isEmpty()) {
            return getHotProducts(topN);
        }

        return productMapper.selectBatchIds(recommendIds);
    }

    /**
     * 记录用户行为（浏览/收藏/购买/评分）。
     * 浏览行为做 30 分钟去重，防止短时间重复刷新导致权重失真。
     */
    public void recordBehavior(Long userId, Long productId, int behaviorType) {
        // 浏览行为去重（同一用户同一产品短时间内只记录一次）
        if (behaviorType == Constants.BEHAVIOR_VIEW) {
            LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserBehavior::getUserId, userId)
                    .eq(UserBehavior::getProductId, productId)
                    .eq(UserBehavior::getBehaviorType, Constants.BEHAVIOR_VIEW)
                    .orderByDesc(UserBehavior::getCreateTime)
                    .last("LIMIT 1");
            UserBehavior last = behaviorMapper.selectOne(wrapper);
            if (last != null && last.getCreateTime().plusMinutes(30).isAfter(java.time.LocalDateTime.now())) {
                return;
            }
        }

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setProductId(productId);
        behavior.setBehaviorType(behaviorType);
        behaviorMapper.insert(behavior);
    }

    /**
     * 定时重建产品相似度矩阵。
     * 说明：这里使用“全量重算 + 原子替换引用”，保证读请求始终拿到完整矩阵。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void refreshSimilarityMatrix() {
        log.info("开始刷新物品相似度矩阵...");

        // 1. 获取所有行为数据
        List<UserBehavior> allBehaviors = behaviorMapper.selectList(null);
        if (allBehaviors.isEmpty()) {
            log.info("无行为数据，跳过计算");
            return;
        }

        // 2. 构建用户-物品评分矩阵
        // Map<userId, Map<productId, score>>
        Map<Long, Map<Long, Double>> ratingMatrix = new HashMap<>();
        for (UserBehavior behavior : allBehaviors) {
            ratingMatrix.computeIfAbsent(behavior.getUserId(), k -> new HashMap<>());
            Map<Long, Double> userMap = ratingMatrix.get(behavior.getUserId());

            double weight = switch (behavior.getBehaviorType()) {
                case 0 -> Constants.WEIGHT_VIEW;
                case 1 -> Constants.WEIGHT_FAVORITE;
                case 2 -> Constants.WEIGHT_PURCHASE;
                case 3 -> behavior.getScore() != null ? behavior.getScore().doubleValue() : 3.0;
                default -> 1.0;
            };

            // 取最大权重
            userMap.merge(behavior.getProductId(), weight, Math::max);
        }

        // 3. 计算物品相似度（余弦相似度）
        // 先构建物品-用户倒排索引
        Map<Long, Map<Long, Double>> itemUserMap = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> userEntry : ratingMatrix.entrySet()) {
            Long userId = userEntry.getKey();
            for (Map.Entry<Long, Double> itemEntry : userEntry.getValue().entrySet()) {
                itemUserMap.computeIfAbsent(itemEntry.getKey(), k -> new HashMap<>())
                        .put(userId, itemEntry.getValue());
            }
        }

        Map<Long, Map<Long, Double>> newMatrix = new ConcurrentHashMap<>();
        List<Long> itemIds = new ArrayList<>(itemUserMap.keySet());

        for (int i = 0; i < itemIds.size(); i++) {
            Long itemA = itemIds.get(i);
            Map<Long, Double> usersA = itemUserMap.get(itemA);

            for (int j = i + 1; j < itemIds.size(); j++) {
                Long itemB = itemIds.get(j);
                Map<Long, Double> usersB = itemUserMap.get(itemB);

                // 余弦相似度 = 向量点积 / (模长乘积)
                double dotProduct = 0, normA = 0, normB = 0;
                Set<Long> commonUsers = new HashSet<>(usersA.keySet());
                commonUsers.retainAll(usersB.keySet());

                if (commonUsers.isEmpty()) continue;

                for (Long uid : commonUsers) {
                    dotProduct += usersA.get(uid) * usersB.get(uid);
                }
                for (double v : usersA.values()) normA += v * v;
                for (double v : usersB.values()) normB += v * v;

                double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));

                if (similarity > 0.1) { // 过滤低相似度
                    newMatrix.computeIfAbsent(itemA, k -> new ConcurrentHashMap<>()).put(itemB, similarity);
                    newMatrix.computeIfAbsent(itemB, k -> new ConcurrentHashMap<>()).put(itemA, similarity);
                }
            }
        }

        this.itemSimilarityMatrix = newMatrix;
        log.info("物品相似度矩阵刷新完成，共 {} 个物品", itemIds.size());
    }

    /**
     * 将用户行为序列转换成“产品偏好强度”映射。
     *
     * 权重规则：
     * 浏览 < 收藏 < 购买 < 显式评分。
     * 同一产品有多条行为时取最大权重，避免重复行为过度放大。
     */
    private Map<Long, Double> buildUserRatingMap(List<UserBehavior> behaviors) {
        Map<Long, Double> ratings = new HashMap<>();
        for (UserBehavior b : behaviors) {
            double weight = switch (b.getBehaviorType()) {
                case 0 -> Constants.WEIGHT_VIEW;
                case 1 -> Constants.WEIGHT_FAVORITE;
                case 2 -> Constants.WEIGHT_PURCHASE;
                case 3 -> b.getScore() != null ? b.getScore().doubleValue() : 3.0;
                default -> 1.0;
            };
            ratings.merge(b.getProductId(), weight, Math::max);
        }
        return ratings;
    }

    /**
     * 热门产品兜底策略：按销量倒序取前 N 条。
     */
    private List<TravelProduct> getHotProducts(int topN) {
        LambdaQueryWrapper<TravelProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelProduct::getStatus, 1);
        wrapper.orderByDesc(TravelProduct::getSales);
        wrapper.last("LIMIT " + topN);
        return productMapper.selectList(wrapper);
    }
}
