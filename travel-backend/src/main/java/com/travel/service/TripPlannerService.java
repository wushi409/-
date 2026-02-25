package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.*;
import com.travel.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripPlannerService {

    private final CustomRequestMapper requestMapper;
    private final TravelProductMapper productMapper;
    private final AttractionMapper attractionMapper;
    private final HotelMapper hotelMapper;
    private final DestinationMapper destinationMapper;

    // 景点冷却期（天数）：同一景点隔几天后可再次安排
    private static final int ATTRACTION_COOLDOWN = 3;
    // 产品冷却期
    private static final int PRODUCT_COOLDOWN = 5;
    // 推荐的最大行程天数（超过此天数会提示用户）
    private static final int MAX_RECOMMENDED_DAYS = 14;

    /**
     * 根据定制需求自动生成旅游方案。
     *
     * 算法特性：
     * 1. 预算约束：按“总预算/天数”限制单日可分配资源；
     * 2. 偏好约束：优先分配与兴趣标签匹配度高的景点/产品；
     * 3. 多样性约束：通过冷却期避免同一资源在短期内反复出现；
     * 4. 兜底策略：当资源不足时自动补齐全局资源并生成自由活动项。
     */
    public GeneratedPlan generatePlan(Long requestId) {
        // 1. 获取定制需求
        CustomRequest request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new RuntimeException("定制需求不存在");
        }

        // 2. 计算行程天数（限制在合理范围内）
        int days = calculateDays(request.getStartDate(), request.getEndDate());
        if (days <= 0) days = 3; // 默认3天
        if (days > MAX_RECOMMENDED_DAYS) {
            log.warn("行程天数{}天超过推荐上限{}天，将采用轮换策略安排", days, MAX_RECOMMENDED_DAYS);
        }

        // 3. 计算每日预算
        BigDecimal totalBudget = request.getBudgetMax() != null ? request.getBudgetMax() : new BigDecimal("5000");
        BigDecimal dailyBudget = totalBudget.divide(new BigDecimal(days), 2, RoundingMode.HALF_UP);

        // 4. 获取目的地信息
        Long destinationId = request.getDestinationId();
        Destination destination = destinationId != null ? destinationMapper.selectById(destinationId) : null;

        // 5. 获取可用资源（如果目的地资源不足，也获取全局热门资源）
        List<Attraction> attractions = getAvailableAttractions(destinationId);
        List<Hotel> hotels = getAvailableHotels(destinationId);
        List<TravelProduct> products = getAvailableProducts(destinationId, request.getInterestTags());

        // 如果目的地资源不足，补充全局资源
        if (attractions.size() < 5 && destinationId != null) {
            List<Attraction> globalAttractions = getAvailableAttractions(null);
            attractions.addAll(globalAttractions.stream()
                    .filter(a -> !attractions.contains(a))
                    .limit(10)
                    .collect(Collectors.toList()));
        }
        if (products.size() < 3 && destinationId != null) {
            List<TravelProduct> globalProducts = getAvailableProducts(null, request.getInterestTags());
            products.addAll(globalProducts.stream()
                    .filter(p -> !products.contains(p))
                    .limit(5)
                    .collect(Collectors.toList()));
        }

        // 6. 解析用户兴趣标签
        Set<String> interestTags = parseInterestTags(request.getInterestTags());

        // 7. 使用轮换+贪心算法生成每日行程
        List<DayPlan> dayPlans = new ArrayList<>();
        // 记录每个资源最后使用的天数（用于冷却期计算）
        Map<Long, Integer> attractionLastUsedDay = new HashMap<>();
        Map<Long, Integer> productLastUsedDay = new HashMap<>();
        BigDecimal totalCost = BigDecimal.ZERO;

        // 按兴趣标签对景点排序，创建轮换队列
        List<Attraction> sortedAttractions = sortByInterestMatch(attractions, interestTags);
        List<TravelProduct> sortedProducts = sortProductsByInterest(products, interestTags);

        for (int day = 1; day <= days; day++) {
            DayPlan dayPlan = new DayPlan();
            dayPlan.setDayNumber(day);
            dayPlan.setDate(request.getStartDate() != null ? request.getStartDate().plusDays(day - 1) : null);
            
            BigDecimal dayBudgetRemaining = dailyBudget;
            List<PlanItem> items = new ArrayList<>();

            // 7.1 分配景点（每天1-2个，使用轮换策略）
            int attractionCount = Math.min(2, Math.max(1, sortedAttractions.size()));
            List<Attraction> dayAttractions = selectAttractionsWithCooldown(
                    sortedAttractions, attractionLastUsedDay, day, 
                    dayBudgetRemaining, interestTags, attractionCount);
            
            for (Attraction attr : dayAttractions) {
                PlanItem item = new PlanItem();
                item.setType("attraction");
                item.setName(attr.getName());
                item.setDescription(attr.getDescription());
                item.setPrice(attr.getTicketPrice());
                item.setTime(attr.getOpenTime());
                item.setImageUrl(attr.getCoverImage());
                items.add(item);
                
                attractionLastUsedDay.put(attr.getId(), day);
                if (attr.getTicketPrice() != null) {
                    dayBudgetRemaining = dayBudgetRemaining.subtract(attr.getTicketPrice());
                    totalCost = totalCost.add(attr.getTicketPrice());
                }
            }

            // 7.2 分配旅游产品（每天0-1个）
            TravelProduct selectedProduct = selectProductWithCooldown(
                    sortedProducts, productLastUsedDay, day, 
                    dayBudgetRemaining, interestTags);
            if (selectedProduct != null) {
                PlanItem item = new PlanItem();
                item.setType("product");
                item.setName(selectedProduct.getTitle());
                item.setDescription(selectedProduct.getDescription());
                item.setPrice(selectedProduct.getPrice());
                item.setProductId(selectedProduct.getId());
                // item.setImageUrl(selectedProduct.getImageUrl()); // 如果有图片字段再启用
                items.add(item);
                
                productLastUsedDay.put(selectedProduct.getId(), day);
                if (selectedProduct.getPrice() != null) {
                    totalCost = totalCost.add(selectedProduct.getPrice());
                }
            }

            // 7.3 如果当天没有安排，添加推荐活动
            if (items.isEmpty()) {
                items.add(createFreeActivityItem(day, destination, interestTags));
            }

            // 7.4 分配酒店（每天1个，最后一天除外）
            if (day < days) {
                Hotel selectedHotel = selectHotel(hotels, dayBudgetRemaining, day);
                if (selectedHotel != null) {
                    dayPlan.setHotelName(selectedHotel.getName());
                    dayPlan.setHotelPrice(selectedHotel.getPriceMin());
                    if (selectedHotel.getPriceMin() != null) {
                        totalCost = totalCost.add(selectedHotel.getPriceMin());
                    }
                }
            }

            dayPlan.setItems(items);
            dayPlan.setTitle(generateDayTitle(day, items, destination));
            dayPlans.add(dayPlan);
        }

        // 8. 构建最终方案
        GeneratedPlan plan = new GeneratedPlan();
        plan.setTitle(generatePlanTitle(destination, days));
        plan.setDescription(generatePlanDescription(request, destination, days, sortedAttractions.size(), sortedProducts.size()));
        plan.setDayPlans(dayPlans);
        plan.setTotalPrice(totalCost.multiply(new BigDecimal(request.getPeopleCount() != null ? request.getPeopleCount() : 1)));
        plan.setDays(days);
        plan.setPeopleCount(request.getPeopleCount());
        plan.setHighlights(generateHighlights(dayPlans));

        log.info("成功为定制需求[{}]生成{}天行程方案，总价：{}", requestId, days, plan.getTotalPrice());
        return plan;
    }

    /**
     * 带冷却期的景点选择（轮换策略）
     */
    private List<Attraction> selectAttractionsWithCooldown(
            List<Attraction> all, Map<Long, Integer> lastUsedDay, int currentDay,
            BigDecimal budget, Set<String> interests, int maxCount) {
        
        return all.stream()
                .filter(a -> {
                    // 检查冷却期：如果从未使用或已过冷却期，则可用
                    Integer lastDay = lastUsedDay.get(a.getId());
                    return lastDay == null || (currentDay - lastDay) >= ATTRACTION_COOLDOWN;
                })
                .filter(a -> a.getTicketPrice() == null || a.getTicketPrice().compareTo(budget) <= 0)
                .sorted((a, b) -> {
                    // 优先选择未使用过的
                    boolean aUsed = lastUsedDay.containsKey(a.getId());
                    boolean bUsed = lastUsedDay.containsKey(b.getId());
                    if (aUsed != bUsed) return aUsed ? 1 : -1;
                    
                    // 其次按兴趣匹配度
                    int scoreA = calculateTagMatchScore(a.getTags(), interests);
                    int scoreB = calculateTagMatchScore(b.getTags(), interests);
                    if (scoreA != scoreB) return scoreB - scoreA;
                    
                    // 最后按价格（性价比）
                    BigDecimal priceA = a.getTicketPrice() != null ? a.getTicketPrice() : BigDecimal.ZERO;
                    BigDecimal priceB = b.getTicketPrice() != null ? b.getTicketPrice() : BigDecimal.ZERO;
                    return priceA.compareTo(priceB);
                })
                .limit(maxCount)
                .collect(Collectors.toList());
    }

    /**
     * 带冷却期的产品选择。
     *
     * 排序优先级：
     * 1. 未在近期使用（保证新鲜度）；
     * 2. 兴趣标签匹配度；
     * 3. 销量（作为受欢迎程度的弱信号）。
     */
    private TravelProduct selectProductWithCooldown(
            List<TravelProduct> all, Map<Long, Integer> lastUsedDay, int currentDay,
            BigDecimal budget, Set<String> interests) {
        
        return all.stream()
                .filter(p -> {
                    Integer lastDay = lastUsedDay.get(p.getId());
                    return lastDay == null || (currentDay - lastDay) >= PRODUCT_COOLDOWN;
                })
                .filter(p -> p.getStock() > 0)
                .filter(p -> p.getPrice() == null || p.getPrice().compareTo(budget) <= 0)
                .max((a, b) -> {
                    // 优先选择未使用过的
                    boolean aUsed = lastUsedDay.containsKey(a.getId());
                    boolean bUsed = lastUsedDay.containsKey(b.getId());
                    if (aUsed != bUsed) return aUsed ? -1 : 1;
                    
                    int scoreA = calculateTagMatchScore(a.getTags(), interests);
                    int scoreB = calculateTagMatchScore(b.getTags(), interests);
                    if (scoreA != scoreB) return scoreA - scoreB;
                    
                    return (a.getSales() != null ? a.getSales() : 0) - (b.getSales() != null ? b.getSales() : 0);
                })
                .orElse(null);
    }

    /**
     * 按兴趣匹配度排序景点
     */
    private List<Attraction> sortByInterestMatch(List<Attraction> attractions, Set<String> interests) {
        return attractions.stream()
                .sorted((a, b) -> {
                    int scoreA = calculateTagMatchScore(a.getTags(), interests);
                    int scoreB = calculateTagMatchScore(b.getTags(), interests);
                    return scoreB - scoreA;
                })
                .collect(Collectors.toList());
    }

    /**
     * 按兴趣匹配度排序产品
     */
    private List<TravelProduct> sortProductsByInterest(List<TravelProduct> products, Set<String> interests) {
        return products.stream()
                .sorted((a, b) -> {
                    int scoreA = calculateTagMatchScore(a.getTags(), interests);
                    int scoreB = calculateTagMatchScore(b.getTags(), interests);
                    if (scoreA != scoreB) return scoreB - scoreA;
                    return (b.getSales() != null ? b.getSales() : 0) - (a.getSales() != null ? a.getSales() : 0);
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建自由活动项目（当天没有其他安排时）
     */
    private PlanItem createFreeActivityItem(int day, Destination dest, Set<String> interests) {
        PlanItem item = new PlanItem();
        item.setType("activity");
        
        // 根据兴趣标签生成推荐活动
        String destName = dest != null ? dest.getName() : "当地";
        List<String> activities = new ArrayList<>();
        
        if (interests.contains("美食")) {
            activities.add(destName + "特色美食探索");
        }
        if (interests.contains("文化") || interests.contains("历史")) {
            activities.add(destName + "文化街区漫步");
        }
        if (interests.contains("海滩") || interests.contains("自然")) {
            activities.add("海滨休闲时光");
        }
        if (interests.contains("购物")) {
            activities.add(destName + "购物中心自由购物");
        }
        if (interests.contains("摄影")) {
            activities.add("城市风光摄影之旅");
        }
        
        if (activities.isEmpty()) {
            // 默认活动轮换
            String[] defaultActivities = {
                destName + "城市自由探索",
                "休闲放松・自由活动",
                destName + "周边探索",
                "当地市场体验",
                "休闲时光・品味当地生活"
            };
            activities.add(defaultActivities[(day - 1) % defaultActivities.length]);
        }
        
        item.setName(activities.get(0));
        item.setDescription("自由安排时间，可根据个人兴趣探索" + destName + "的独特魅力");
        item.setPrice(BigDecimal.ZERO);
        return item;
    }

    /**
     * 选择酒店（轮换不同酒店增加体验多样性）
     */
    private Hotel selectHotel(List<Hotel> hotels, BigDecimal budget, int day) {
        if (hotels.isEmpty()) return null;
        
        // 过滤预算内的酒店
        List<Hotel> affordableHotels = hotels.stream()
                .filter(h -> h.getPriceMin() == null || h.getPriceMin().compareTo(budget) <= 0)
                .collect(Collectors.toList());
        
        if (affordableHotels.isEmpty()) {
            return hotels.get(0); // 如果都超预算，返回第一个
        }
        
        // 轮换选择酒店（如果有多个的话）
        int index = (day - 1) % affordableHotels.size();
        return affordableHotels.get(index);
    }

    /**
     * 计算标签匹配分数（简单关键字包含）。
     * 当前实现偏轻量，后续可替换为分词/同义词模型提升匹配准确性。
     */
    private int calculateTagMatchScore(String itemTags, Set<String> userInterests) {
        if (itemTags == null || userInterests.isEmpty()) return 0;
        int score = 0;
        for (String interest : userInterests) {
            if (itemTags.toLowerCase().contains(interest.toLowerCase())) score++;
        }
        return score;
    }

    /**
     * 获取可用景点
     */
    private List<Attraction> getAvailableAttractions(Long destinationId) {
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attraction::getStatus, 1);
        if (destinationId != null) {
            wrapper.eq(Attraction::getDestinationId, destinationId);
        }
        wrapper.orderByDesc(Attraction::getCreateTime);
        return attractionMapper.selectList(wrapper);
    }

    /**
     * 获取可用酒店
     */
    private List<Hotel> getAvailableHotels(Long destinationId) {
        LambdaQueryWrapper<Hotel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Hotel::getStatus, 1);
        if (destinationId != null) {
            wrapper.eq(Hotel::getDestinationId, destinationId);
        }
        wrapper.orderByDesc(Hotel::getStarLevel);
        return hotelMapper.selectList(wrapper);
    }

    /**
     * 获取可用产品。
     * 仅返回“上架且有库存”的产品，避免把不可售产品放入规划结果。
     */
    private List<TravelProduct> getAvailableProducts(Long destinationId, String interestTags) {
        LambdaQueryWrapper<TravelProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelProduct::getStatus, 1);
        wrapper.gt(TravelProduct::getStock, 0);
        if (destinationId != null) {
            wrapper.eq(TravelProduct::getDestinationId, destinationId);
        }
        wrapper.orderByDesc(TravelProduct::getSales);
        return productMapper.selectList(wrapper);
    }

    /**
     * 解析兴趣标签
     */
    private Set<String> parseInterestTags(String tags) {
        if (tags == null || tags.isEmpty()) return Collections.emptySet();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 计算行程天数
     */
    private int calculateDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    /**
     * 生成每日标题
     */
    private String generateDayTitle(int day, List<PlanItem> items, Destination dest) {
        if (items.isEmpty()) {
            return "第" + day + "天 - 自由活动";
        }
        String mainItem = items.get(0).getName();
        if (mainItem.length() > 15) mainItem = mainItem.substring(0, 15) + "...";
        return "第" + day + "天 - " + mainItem;
    }

    /**
     * 生成方案标题
     */
    private String generatePlanTitle(Destination dest, int days) {
        String destName = dest != null ? dest.getName() : "精选";
        if (days <= 3) {
            return destName + days + "天" + (days - 1) + "晚精致短途游";
        } else if (days <= 7) {
            return destName + days + "天" + (days - 1) + "晚深度体验游";
        } else {
            return destName + days + "天" + (days - 1) + "晚深度探索之旅";
        }
    }

    /**
     * 生成方案描述文案。
     * 文案中会回显用户约束与资源统计，便于用户理解“为什么推荐这些内容”。
     */
    private String generatePlanDescription(CustomRequest req, Destination dest, int days, int attractionCount, int productCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据您的定制需求，为您智能规划的").append(days).append("天旅行方案。\n\n");
        if (dest != null) {
            sb.append("【目的地】").append(dest.getName()).append("\n");
        }
        if (req.getPeopleCount() != null) {
            sb.append("【出行人数】").append(req.getPeopleCount()).append("人\n");
        }
        if (req.getInterestTags() != null && !req.getInterestTags().isEmpty()) {
            sb.append("【兴趣偏好】").append(req.getInterestTags()).append("\n");
        }
        sb.append("【资源统计】").append(attractionCount).append("个景点, ").append(productCount).append("个产品可选\n");
        sb.append("\n本方案由智能算法根据您的预算和偏好自动生成，采用轮换策略确保行程丰富多样。您可以与服务商沟通进一步调整。");
        return sb.toString();
    }

    /**
     * 生成行程亮点
     */
    private List<String> generateHighlights(List<DayPlan> dayPlans) {
        Set<String> highlights = new LinkedHashSet<>();
        for (DayPlan day : dayPlans) {
            for (PlanItem item : day.getItems()) {
                if ("attraction".equals(item.getType()) && item.getName() != null) {
                    highlights.add(item.getName());
                    if (highlights.size() >= 5) break;
                }
            }
            if (highlights.size() >= 5) break;
        }
        return new ArrayList<>(highlights);
    }

    // ===== 内部数据类 =====

    @Data
    public static class GeneratedPlan {
        private String title;
        private String description;
        private List<DayPlan> dayPlans;
        private BigDecimal totalPrice;
        private Integer days;
        private Integer peopleCount;
        private List<String> highlights; // 行程亮点
    }

    @Data
    public static class DayPlan {
        private Integer dayNumber;
        private LocalDate date;
        private String title;
        private List<PlanItem> items;
        private String hotelName;
        private BigDecimal hotelPrice;
    }

    @Data
    public static class PlanItem {
        private String type; // attraction, product, activity
        private String name;
        private String description;
        private BigDecimal price;
        private String time;
        private Long productId;
        private String imageUrl;
    }
}
