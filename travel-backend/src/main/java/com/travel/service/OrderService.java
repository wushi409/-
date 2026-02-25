package com.travel.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Constants;
import com.travel.common.PageResult;
import com.travel.common.exception.BusinessException;
import com.travel.entity.TravelOrder;
import com.travel.entity.TravelProduct;
import com.travel.entity.User;
import com.travel.mapper.TravelOrderMapper;
import com.travel.mapper.TravelProductMapper;
import com.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单领域服务。
 *
 * 设计说明：
 * 1. 统一承载“下单、支付、履约、退款”的状态流转校验；
 * 2. 在关键状态变更时联动产品总库存/销量与按日库存，保证数据一致性；
 * 3. 提供游客、服务商、管理员三种视角的订单查询能力。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final TravelOrderMapper orderMapper;
    private final TravelProductMapper productMapper;
    private final UserMapper userMapper;
    private final DailyStockService dailyStockService;

    /**
     * 创建订单（初始状态：待付款）。
     *
     * 关键点：
     * 1. 产品单下单时，实时校验产品是否存在、人数是否有效、总库存是否足够；
     * 2. 订单金额按“产品单价 * 出行人数”计算；
     * 3. 仅创建订单记录，不在此阶段扣减库存（库存在支付成功后扣减）。
     */
    public void createOrder(TravelOrder order) {
        order.setOrderNo("T" + IdUtil.getSnowflakeNextIdStr());
        order.setStatus(Constants.ORDER_UNPAID);
        order.setCreateTime(LocalDateTime.now());

        if (order.getProductId() != null) {
            TravelProduct product = productMapper.selectById(order.getProductId());
            if (product == null) {
                throw new BusinessException("product not found");
            }
            if (order.getPeopleCount() == null || order.getPeopleCount() <= 0) {
                throw new BusinessException("invalid people count");
            }
            if (product.getStock() < order.getPeopleCount()) {
                throw new BusinessException("stock not enough");
            }
            order.setProviderId(product.getProviderId());
            order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(order.getPeopleCount())));
            order.setOrderType(0);
        }

        orderMapper.insert(order);
    }

    /**
     * 游客侧订单分页查询。
     * 支持按状态筛选，并按创建时间倒序返回，便于展示最近订单。
     */
    public PageResult<TravelOrder> userOrders(Long userId, Integer page, Integer size, Integer status) {
        Page<TravelOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TravelOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelOrder::getUserId, userId);
        if (status != null) {
            wrapper.eq(TravelOrder::getStatus, status);
        }
        wrapper.orderByDesc(TravelOrder::getCreateTime);
        Page<TravelOrder> result = orderMapper.selectPage(pageParam, wrapper);
        fillOrderInfo(result);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 服务商侧订单分页查询。
     * 仅返回当前服务商名下订单，防止越权查看。
     */
    public PageResult<TravelOrder> providerOrders(Long providerId, Integer page, Integer size, Integer status) {
        Page<TravelOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TravelOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelOrder::getProviderId, providerId);
        if (status != null) {
            wrapper.eq(TravelOrder::getStatus, status);
        }
        wrapper.orderByDesc(TravelOrder::getCreateTime);
        Page<TravelOrder> result = orderMapper.selectPage(pageParam, wrapper);
        fillOrderInfo(result);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 管理员订单分页查询。
     * 支持按状态与关键词（订单号/联系人）检索，便于后台运营排查。
     */
    public PageResult<TravelOrder> adminOrders(Integer page, Integer size, Integer status, String keyword) {
        Page<TravelOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TravelOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(TravelOrder::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(TravelOrder::getOrderNo, keyword)
                    .or().like(TravelOrder::getContactName, keyword));
        }
        wrapper.orderByDesc(TravelOrder::getCreateTime);
        Page<TravelOrder> result = orderMapper.selectPage(pageParam, wrapper);
        fillOrderInfo(result);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 根据订单 ID 查询订单基础信息。
     */
    public TravelOrder getById(Long id) {
        return orderMapper.selectById(id);
    }

    /**
     * 通用状态更新入口。
     *
     * 状态联动规则：
     * 1. 待付款 -> 已付款：记录支付时间并扣减库存；
     * 2. 退款中 -> 已退款：回补库存与销量。
     */
    public void updateStatus(Long orderId, Integer status) {
        TravelOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("order not found");
        }

        Integer oldStatus = order.getStatus();
        if (oldStatus != null && oldStatus.equals(status)) {
            return;
        }

        if (status == Constants.ORDER_PAID && oldStatus != null && oldStatus == Constants.ORDER_UNPAID) {
            order.setPayTime(LocalDateTime.now());
            adjustProductOnPay(order);
        }

        if (status == Constants.ORDER_REFUNDED && oldStatus != null && oldStatus == Constants.ORDER_REFUNDING) {
            adjustProductOnRefund(order);
        }

        order.setStatus(status);
        orderMapper.updateById(order);
    }

    /**
     * 游客取消订单。
     * 仅允许取消“待付款”订单，避免已支付订单被直接取消导致账实不一致。
     */
    public void cancelOrder(Long orderId, Long userId) {
        TravelOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("order not found");
        }
        if (order.getStatus() != Constants.ORDER_UNPAID) {
            throw new BusinessException("only unpaid orders can be cancelled");
        }
        order.setStatus(Constants.ORDER_CANCELLED);
        orderMapper.updateById(order);
    }

    /**
     * 游客发起退款申请。
     * 仅“已付款/进行中”订单可进入退款流程，其他状态拒绝申请。
     */
    public void requestRefund(Long orderId, Long userId) {
        TravelOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("order not found");
        }
        if (order.getStatus() != Constants.ORDER_PAID && order.getStatus() != Constants.ORDER_IN_PROGRESS) {
            throw new BusinessException("current order status cannot request refund");
        }
        order.setStatus(Constants.ORDER_REFUNDING);
        orderMapper.updateById(order);
    }

    /**
     * 服务商更新订单状态。
     *
     * 状态机约束：
     * 1. 已付款 -> 进行中；
     * 2. 进行中 -> 已完成；
     * 3. 退款中 -> 已付款（拒绝退款）或 已退款（同意退款）。
     */
    public void providerUpdateStatus(Long orderId, Long providerId, Integer status) {
        TravelOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("order not found");
        }
        if (!order.getProviderId().equals(providerId)) {
            throw new BusinessException("no permission to operate this order");
        }

        Integer oldStatus = order.getStatus();
        if (status == Constants.ORDER_IN_PROGRESS && oldStatus != Constants.ORDER_PAID) {
            throw new BusinessException("only paid orders can be started");
        }
        if (status == Constants.ORDER_COMPLETED && oldStatus != Constants.ORDER_IN_PROGRESS) {
            throw new BusinessException("only in-progress orders can be completed");
        }
        if (status == Constants.ORDER_PAID && oldStatus != Constants.ORDER_REFUNDING) {
            throw new BusinessException("only refunding orders can be reverted to paid");
        }
        if (status == Constants.ORDER_REFUNDED && oldStatus != Constants.ORDER_REFUNDING) {
            throw new BusinessException("only refunding orders can be marked refunded");
        }

        updateStatus(orderId, status);
    }

    /**
     * 服务商处理退款申请。
     * approve=true 表示同意退款；false 表示驳回并恢复为“已付款”。
     */
    public void providerHandleRefund(Long orderId, Long providerId, boolean approve) {
        providerUpdateStatus(orderId, providerId, approve ? Constants.ORDER_REFUNDED : Constants.ORDER_PAID);
    }

    /**
     * 支付成功后的库存与销量联动。
     *
     * 处理顺序：
     * 1. 扣减产品总库存并增加销量；
     * 2. 同步扣减按日库存（用于具体出行日库存预警/控制）。
     */
    private void adjustProductOnPay(TravelOrder order) {
        if (order.getProductId() == null) {
            return;
        }
        TravelProduct product = productMapper.selectById(order.getProductId());
        if (product == null) {
            return;
        }
        if (product.getStock() < order.getPeopleCount()) {
            throw new BusinessException("stock not enough");
        }
        product.setSales(product.getSales() + order.getPeopleCount());
        product.setStock(product.getStock() - order.getPeopleCount());
        productMapper.updateById(product);

        dailyStockService.decreaseOnPay(order.getProductId(), order.getTravelDate(), order.getPeopleCount());
    }

    /**
     * 退款成功后的库存与销量回滚。
     *
     * 处理顺序：
     * 1. 回补产品总库存并回退销量（最低不小于 0）；
     * 2. 同步回补按日库存。
     */
    private void adjustProductOnRefund(TravelOrder order) {
        if (order.getProductId() == null) {
            return;
        }
        TravelProduct product = productMapper.selectById(order.getProductId());
        if (product == null) {
            return;
        }
        product.setStock(product.getStock() + order.getPeopleCount());
        int nextSales = product.getSales() - order.getPeopleCount();
        product.setSales(Math.max(0, nextSales));
        productMapper.updateById(product);

        dailyStockService.increaseOnRefund(order.getProductId(), order.getTravelDate(), order.getPeopleCount());
    }

    /**
     * 订单列表补充展示字段（用户名、产品名、服务商名）。
     * 当前实现为逐条查询，数据量较大时可优化为批量查询以减少 SQL 次数。
     */
    private void fillOrderInfo(Page<TravelOrder> result) {
        result.getRecords().forEach(order -> {
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                order.setUserName(user.getNickname());
            }
            if (order.getProductId() != null) {
                TravelProduct product = productMapper.selectById(order.getProductId());
                if (product != null) {
                    order.setProductTitle(product.getTitle());
                }
            }
            User provider = userMapper.selectById(order.getProviderId());
            if (provider != null) {
                order.setProviderName(provider.getNickname());
            }
        });
    }
}
