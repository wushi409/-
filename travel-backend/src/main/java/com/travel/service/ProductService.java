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

    public TravelProduct getById(Long id) {
        TravelProduct product = productMapper.selectById(id);
        if (product != null) {
            fillSingleProductInfo(product);
        }
        return product;
    }

    public void add(TravelProduct product) {
        product.setStatus(1);
        product.setSales(0);
        productMapper.insert(product);
    }

    public void update(TravelProduct product) {
        productMapper.updateById(product);
    }

    public void delete(Long id) {
        productMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status) {
        TravelProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("product not found");
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    public void updateByProvider(Long providerId, TravelProduct product) {
        TravelProduct existing = assertProviderProduct(providerId, product.getId());
        product.setProviderId(existing.getProviderId());
        productMapper.updateById(product);
    }

    public void updateStatusByProvider(Long providerId, Long id, Integer status) {
        TravelProduct product = assertProviderProduct(providerId, id);
        product.setStatus(status);
        productMapper.updateById(product);
    }

    public void deleteByProvider(Long providerId, Long id) {
        assertProviderProduct(providerId, id);
        productMapper.deleteById(id);
    }

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

        private void fillProductInfo(List<TravelProduct> products) {
        products.forEach(this::fillSingleProductInfo);
    }

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
