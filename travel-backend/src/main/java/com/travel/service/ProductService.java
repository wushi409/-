package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.common.exception.BusinessException;
import com.travel.entity.Destination;
import com.travel.entity.TravelProduct;
import com.travel.entity.User;
import com.travel.mapper.DestinationMapper;
import com.travel.mapper.TravelProductMapper;
import com.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 类说明：ProductService
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final TravelProductMapper productMapper;
    private final DestinationMapper destinationMapper;
    private final UserMapper userMapper;

    public PageResult<TravelProduct> list(Integer page, Integer size, String keyword,
                                          Long destinationId, Integer productType,
                                          BigDecimal priceMin, BigDecimal priceMax,
                                          String sortBy) {
        Page<TravelProduct> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TravelProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelProduct::getStatus, 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(TravelProduct::getTitle, keyword)
                    .or().like(TravelProduct::getTags, keyword));
        }
        if (destinationId != null) {
            wrapper.eq(TravelProduct::getDestinationId, destinationId);
        }
        if (productType != null) {
            wrapper.eq(TravelProduct::getProductType, productType);
        }
        if (priceMin != null) {
            wrapper.ge(TravelProduct::getPrice, priceMin);
        }
        if (priceMax != null) {
            wrapper.le(TravelProduct::getPrice, priceMax);
        }
        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(TravelProduct::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(TravelProduct::getPrice);
        } else if ("sales".equals(sortBy)) {
            wrapper.orderByDesc(TravelProduct::getSales);
        } else {
            wrapper.orderByDesc(TravelProduct::getCreateTime);
        }
        Page<TravelProduct> result = productMapper.selectPage(pageParam, wrapper);
        fillProductInfo(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<TravelProduct> adminList(Integer page, Integer size, String keyword,
                                               Long destinationId, Long providerId, Integer status) {
        Page<TravelProduct> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TravelProduct> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(TravelProduct::getTitle, keyword);
        }
        if (destinationId != null) {
            wrapper.eq(TravelProduct::getDestinationId, destinationId);
        }
        if (providerId != null) {
            wrapper.eq(TravelProduct::getProviderId, providerId);
        }
        if (status != null) {
            wrapper.eq(TravelProduct::getStatus, status);
        }
        wrapper.orderByDesc(TravelProduct::getCreateTime);
        Page<TravelProduct> result = productMapper.selectPage(pageParam, wrapper);
        fillProductInfo(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 方法说明：getById
     * 1. 负责处理 getById 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public TravelProduct getById(Long id) {
        TravelProduct product = productMapper.selectById(id);
        if (product != null) {
            fillSingleProductInfo(product);
        }
        return product;
    }

    /**
     * 方法说明：add
     * 1. 负责处理 add 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void add(TravelProduct product) {
        product.setStatus(1);
        product.setSales(0);
        productMapper.insert(product);
    }

    /**
     * 方法说明：update
     * 1. 负责处理 update 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void update(TravelProduct product) {
        productMapper.updateById(product);
    }

    /**
     * 方法说明：delete
     * 1. 负责处理 delete 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void delete(Long id) {
        productMapper.deleteById(id);
    }

    /**
     * 方法说明：updateStatus
     * 1. 负责处理 updateStatus 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void updateStatus(Long id, Integer status) {
        TravelProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("product not found");
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    /**
     * 方法说明：updateByProvider
     * 1. 负责处理 updateByProvider 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void updateByProvider(Long providerId, TravelProduct product) {
        TravelProduct existing = assertProviderProduct(providerId, product.getId());
        product.setProviderId(existing.getProviderId());
        productMapper.updateById(product);
    }

    /**
     * 方法说明：updateStatusByProvider
     * 1. 负责处理 updateStatusByProvider 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void updateStatusByProvider(Long providerId, Long id, Integer status) {
        TravelProduct product = assertProviderProduct(providerId, id);
        product.setStatus(status);
        productMapper.updateById(product);
    }

    /**
     * 方法说明：deleteByProvider
     * 1. 负责处理 deleteByProvider 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void deleteByProvider(Long providerId, Long id) {
        assertProviderProduct(providerId, id);
        productMapper.deleteById(id);
    }

    /**
     * 方法说明：providerList
     * 1. 负责处理 providerList 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public PageResult<TravelProduct> providerList(Long providerId, Integer page, Integer size, String keyword) {
        Page<TravelProduct> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TravelProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelProduct::getProviderId, providerId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(TravelProduct::getTitle, keyword);
        }
        wrapper.orderByDesc(TravelProduct::getCreateTime);
        Page<TravelProduct> result = productMapper.selectPage(pageParam, wrapper);
        fillProductInfo(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 方法说明：assertProviderProduct
     * 1. 负责处理 assertProviderProduct 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    private TravelProduct assertProviderProduct(Long providerId, Long productId) {
        TravelProduct existing = productMapper.selectById(productId);
        if (existing == null) {
            throw new BusinessException("product not found");
        }
        if (!providerId.equals(existing.getProviderId())) {
            throw new BusinessException("no permission to operate this product");
        }
        return existing;
    }

    /**
     * 方法说明：fillProductInfo
     * 1. 负责处理 fillProductInfo 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    private void fillProductInfo(List<TravelProduct> products) {
        products.forEach(this::fillSingleProductInfo);
    }

    /**
     * 方法说明：fillSingleProductInfo
     * 1. 负责处理 fillSingleProductInfo 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    private void fillSingleProductInfo(TravelProduct product) {
        if (product.getDestinationId() != null) {
            Destination dest = destinationMapper.selectById(product.getDestinationId());
            if (dest != null) {
                product.setDestinationName(dest.getName());
            }
        }
        if (product.getProviderId() != null) {
            User provider = userMapper.selectById(product.getProviderId());
            if (provider != null) {
                product.setProviderName(provider.getNickname());
            }
        }
    }
}
