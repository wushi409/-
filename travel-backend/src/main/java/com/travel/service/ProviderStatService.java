package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Constants;
import com.travel.entity.TravelOrder;
import com.travel.entity.TravelProduct;
import com.travel.mapper.TravelOrderMapper;
import com.travel.mapper.TravelProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProviderStatService {

    private final TravelOrderMapper orderMapper;
    private final TravelProductMapper productMapper;
    private final DailyStockService dailyStockService;

    public Map<String, Object> overview(Long providerId) {
        Map<String, Object> data = new HashMap<>();
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<TravelProduct>()
                .eq(TravelProduct::getProviderId, providerId));
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<TravelOrder>()
                .eq(TravelOrder::getProviderId, providerId));
        Long refundingCount = orderMapper.selectCount(new LambdaQueryWrapper<TravelOrder>()
                .eq(TravelOrder::getProviderId, providerId)
                .eq(TravelOrder::getStatus, Constants.ORDER_REFUNDING));

        List<TravelOrder> paidOrders = orderMapper.selectList(new LambdaQueryWrapper<TravelOrder>()
                .eq(TravelOrder::getProviderId, providerId)
                .in(TravelOrder::getStatus, Constants.ORDER_PAID, Constants.ORDER_IN_PROGRESS, Constants.ORDER_COMPLETED));
        BigDecimal paidAmount = paidOrders.stream()
                .map(TravelOrder::getTotalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("productCount", productCount);
        data.put("orderCount", orderCount);
        data.put("refundingCount", refundingCount);
        data.put("paidAmount", paidAmount);
        data.put("lowStockCount", dailyStockService.countLowStock(providerId));
        return data;
    }

    public List<Map<String, Object>> orderTrend(Long providerId, Integer days) {
        int window = (days == null || days <= 0) ? 30 : days;
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = window - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            Long count = orderMapper.selectCount(new LambdaQueryWrapper<TravelOrder>()
                    .eq(TravelOrder::getProviderId, providerId)
                    .ge(TravelOrder::getCreateTime, start)
                    .lt(TravelOrder::getCreateTime, end));

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> hotProducts(Long providerId, Integer limit) {
        int topN = (limit == null || limit <= 0) ? 10 : limit;
        List<TravelProduct> products = productMapper.selectList(new LambdaQueryWrapper<TravelProduct>()
                .eq(TravelProduct::getProviderId, providerId)
                .orderByDesc(TravelProduct::getSales)
                .last("LIMIT " + topN));

        List<Map<String, Object>> result = new ArrayList<>();
        for (TravelProduct product : products) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId());
            item.put("title", product.getTitle());
            item.put("sales", product.getSales());
            item.put("stock", product.getStock());
            item.put("price", product.getPrice());
            result.add(item);
        }
        return result;
    }
}

