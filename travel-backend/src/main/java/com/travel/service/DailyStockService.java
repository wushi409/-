package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.common.exception.BusinessException;
import com.travel.entity.ProductDailyStock;
import com.travel.entity.TravelProduct;
import com.travel.mapper.ProductDailyStockMapper;
import com.travel.mapper.TravelProductMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按日库存服务。
 *
 * 作用：
 * 1. 提供服务商按日期维护库存总量与预警阈值；
 * 2. 在订单支付/退款时同步扣减或回补“指定出行日”的可用库存；
 * 3. 为前端提供低库存预警列表与统计指标。
 */
@Service
@RequiredArgsConstructor
public class DailyStockService {

    private final ProductDailyStockMapper dailyStockMapper;
    private final TravelProductMapper productMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 启动时确保按日库存表存在。
     * 说明：
     * 1. 部分环境可能只初始化了旧版表结构，导致按日库存相关接口访问时直接 500；
     * 2. 这里通过 IF NOT EXISTS 做幂等建表，不会破坏已有数据；
     * 3. 这样可以保证库存、支付、退款等依赖链路在首次启动后即可正常工作。
     */
    @PostConstruct
    public void ensureDailyStockTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS product_daily_stock ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                + "product_id BIGINT NOT NULL, "
                + "stock_date DATE NOT NULL, "
                + "stock_total INT NOT NULL DEFAULT 0, "
                + "stock_available INT NOT NULL DEFAULT 0, "
                + "warn_threshold INT DEFAULT 0, "
                + "status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用', "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "UNIQUE KEY uk_product_date (product_id, stock_date), "
                + "INDEX idx_product (product_id), "
                + "INDEX idx_stock_date (stock_date)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        jdbcTemplate.execute(ddl);
    }

    public PageResult<ProductDailyStock> listProviderStocks(Long providerId, Long productId,
                                                            LocalDate startDate, LocalDate endDate,
                                                            Integer page, Integer size) {
        Set<Long> providerProductIds = getProviderProductIds(providerId);
        if (providerProductIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, page, size);
        }
        if (productId != null && !providerProductIds.contains(productId)) {
            throw new BusinessException("no permission to operate this product");
        }

        Page<ProductDailyStock> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProductDailyStock> wrapper = new LambdaQueryWrapper<>();
        if (productId != null) {
            wrapper.eq(ProductDailyStock::getProductId, productId);
        } else {
            wrapper.in(ProductDailyStock::getProductId, providerProductIds);
        }
        if (startDate != null) {
            wrapper.ge(ProductDailyStock::getStockDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(ProductDailyStock::getStockDate, endDate);
        }
        wrapper.orderByDesc(ProductDailyStock::getStockDate).orderByDesc(ProductDailyStock::getUpdateTime);
        Page<ProductDailyStock> result = dailyStockMapper.selectPage(pageParam, wrapper);
        fillProductTitle(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 服务商批量新增/更新某个产品的按日库存。
     *
     * 设计要点：
     * 1. 同一产品+日期唯一；
     * 2. 更新总库存时，不允许把已售出的数量“改没了”，因此通过 used 反推最小可用量；
     * 3. warnThreshold 统一修正为 >= 0。
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpsert(Long providerId, BatchStockRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new BusinessException("productId is required");
        }
        assertProviderProduct(providerId, request.getProductId());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("stock items are required");
        }

        for (DailyStockItem item : request.getItems()) {
            if (item.getStockDate() == null) {
                continue;
            }
            if (item.getStockTotal() == null || item.getStockTotal() < 0) {
                throw new BusinessException("stockTotal is invalid");
            }
            int warnThreshold = item.getWarnThreshold() == null ? 0 : Math.max(0, item.getWarnThreshold());

            ProductDailyStock existing = dailyStockMapper.selectOne(new LambdaQueryWrapper<ProductDailyStock>()
                    .eq(ProductDailyStock::getProductId, request.getProductId())
                    .eq(ProductDailyStock::getStockDate, item.getStockDate())
                    .last("LIMIT 1"));

            if (existing == null) {
                ProductDailyStock entity = new ProductDailyStock();
                entity.setProductId(request.getProductId());
                entity.setStockDate(item.getStockDate());
                entity.setStockTotal(item.getStockTotal());
                entity.setStockAvailable(item.getStockTotal());
                entity.setWarnThreshold(warnThreshold);
                entity.setStatus(1);
                entity.setCreateTime(LocalDateTime.now());
                entity.setUpdateTime(LocalDateTime.now());
                dailyStockMapper.insert(entity);
            } else {
                // 已售数量 = 历史总库存 - 历史可用库存；更新总库存后要保证已售数量仍然成立。
                int used = Math.max(0, existing.getStockTotal() - existing.getStockAvailable());
                // 新可用库存 = 新总库存 - 已售数量，且不小于 0。
                int available = Math.max(0, item.getStockTotal() - used);
                existing.setStockTotal(item.getStockTotal());
                existing.setStockAvailable(available);
                existing.setWarnThreshold(warnThreshold);
                existing.setStatus(1);
                existing.setUpdateTime(LocalDateTime.now());
                dailyStockMapper.updateById(existing);
            }
        }
    }

    /**
     * 低库存预警分页查询。
     * 仅统计“今天及以后”且处于启用状态的库存记录。
     */
    public PageResult<ProductDailyStock> listWarnings(Long providerId, Integer page, Integer size) {
        Set<Long> providerProductIds = getProviderProductIds(providerId);
        if (providerProductIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, page, size);
        }

        Page<ProductDailyStock> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProductDailyStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ProductDailyStock::getProductId, providerProductIds)
                .eq(ProductDailyStock::getStatus, 1)
                .ge(ProductDailyStock::getStockDate, LocalDate.now())
                .apply("(stock_available <= IFNULL(warn_threshold, 0) OR stock_available <= 0)")
                .orderByAsc(ProductDailyStock::getStockDate)
                .orderByAsc(ProductDailyStock::getStockAvailable);
        Page<ProductDailyStock> result = dailyStockMapper.selectPage(pageParam, wrapper);
        fillProductTitle(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 支付成功后扣减按日库存。
     * 若该产品在该出行日未配置按日库存，则跳过（不阻断主流程）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void decreaseOnPay(Long productId, LocalDate travelDate, Integer peopleCount) {
        if (productId == null || travelDate == null || peopleCount == null || peopleCount <= 0) {
            return;
        }
        ProductDailyStock stock = dailyStockMapper.selectOne(new LambdaQueryWrapper<ProductDailyStock>()
                .eq(ProductDailyStock::getProductId, productId)
                .eq(ProductDailyStock::getStockDate, travelDate)
                .eq(ProductDailyStock::getStatus, 1)
                .last("LIMIT 1"));
        if (stock == null) {
            return;
        }
        if (stock.getStockAvailable() < peopleCount) {
            throw new BusinessException("daily stock not enough");
        }
        stock.setStockAvailable(stock.getStockAvailable() - peopleCount);
        stock.setUpdateTime(LocalDateTime.now());
        dailyStockMapper.updateById(stock);
    }

    /**
     * 退款成功后回补按日库存。
     * 回补后可用库存不能超过当天总库存上限。
     */
    @Transactional(rollbackFor = Exception.class)
    public void increaseOnRefund(Long productId, LocalDate travelDate, Integer peopleCount) {
        if (productId == null || travelDate == null || peopleCount == null || peopleCount <= 0) {
            return;
        }
        ProductDailyStock stock = dailyStockMapper.selectOne(new LambdaQueryWrapper<ProductDailyStock>()
                .eq(ProductDailyStock::getProductId, productId)
                .eq(ProductDailyStock::getStockDate, travelDate)
                .eq(ProductDailyStock::getStatus, 1)
                .last("LIMIT 1"));
        if (stock == null) {
            return;
        }
        int next = stock.getStockAvailable() + peopleCount;
        stock.setStockAvailable(Math.min(stock.getStockTotal(), next));
        stock.setUpdateTime(LocalDateTime.now());
        dailyStockMapper.updateById(stock);
    }

    /**
     * 统计服务商当前低库存记录条数（用于仪表盘角标）。
     */
    public Long countLowStock(Long providerId) {
        Set<Long> providerProductIds = getProviderProductIds(providerId);
        if (providerProductIds.isEmpty()) {
            return 0L;
        }
        return dailyStockMapper.selectCount(new LambdaQueryWrapper<ProductDailyStock>()
                .in(ProductDailyStock::getProductId, providerProductIds)
                .eq(ProductDailyStock::getStatus, 1)
                .ge(ProductDailyStock::getStockDate, LocalDate.now())
                .apply("(stock_available <= IFNULL(warn_threshold, 0) OR stock_available <= 0)"));
    }

    /**
     * 获取服务商名下所有产品 ID，用于数据权限过滤。
     */
    private Set<Long> getProviderProductIds(Long providerId) {
        return productMapper.selectList(new LambdaQueryWrapper<TravelProduct>()
                        .eq(TravelProduct::getProviderId, providerId))
                .stream()
                .map(TravelProduct::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 校验产品归属，防止服务商越权操作其他服务商库存。
     */
    private void assertProviderProduct(Long providerId, Long productId) {
        TravelProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("product not found");
        }
        if (!providerId.equals(product.getProviderId())) {
            throw new BusinessException("no permission to operate this product");
        }
    }

    /**
     * 回填产品标题，方便前端直接展示。
     */
    private void fillProductTitle(List<ProductDailyStock> records) {
        Set<Long> productIds = records.stream().map(ProductDailyStock::getProductId).collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return;
        }
        Map<Long, String> productTitleMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(TravelProduct::getId, TravelProduct::getTitle));
        records.forEach(item -> item.setProductTitle(productTitleMap.get(item.getProductId())));
    }

    @Data
    public static class BatchStockRequest {
        private Long productId;
        private List<DailyStockItem> items;
    }

    @Data
    public static class DailyStockItem {
        private LocalDate stockDate;
        private Integer stockTotal;
        private Integer warnThreshold;
    }
}
