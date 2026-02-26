package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Constants;
import com.travel.common.exception.BusinessException;
import com.travel.entity.OrderChangeRequest;
import com.travel.entity.TravelOrder;
import com.travel.entity.TravelProduct;
import com.travel.entity.User;
import com.travel.mapper.OrderChangeRequestMapper;
import com.travel.mapper.TravelOrderMapper;
import com.travel.mapper.TravelProductMapper;
import com.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 行程变更申请业务服务。
 */
@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private final OrderChangeRequestMapper orderChangeRequestMapper;
    private final TravelOrderMapper travelOrderMapper;
    private final TravelProductMapper travelProductMapper;
    private final UserMapper userMapper;

    /**
     * 用户创建行程变更申请。
     */
    public OrderChangeRequest create(Long userId, OrderChangeRequest payload) {
        if (payload == null || payload.getOrderId() == null) {
            throw new BusinessException("订单 ID 不能为空");
        }
        if (!StringUtils.hasText(payload.getReason())) {
            throw new BusinessException("变更原因不能为空");
        }

        TravelOrder order = travelOrderMapper.selectById(payload.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException("订单不存在或无权限");
        }
        if (order.getStatus() == null
                || (order.getStatus() != Constants.ORDER_PAID && order.getStatus() != Constants.ORDER_IN_PROGRESS)) {
            throw new BusinessException("当前订单状态不允许发起行程变更");
        }

        // 同一订单同一时刻只允许存在一条待处理申请。
        Long pendingCount = orderChangeRequestMapper.selectCount(
                new LambdaQueryWrapper<OrderChangeRequest>()
                        .eq(OrderChangeRequest::getOrderId, order.getId())
                        .eq(OrderChangeRequest::getStatus, Constants.CHANGE_REQUEST_PENDING));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("该订单已有待处理的变更申请");
        }

        TravelProduct product = order.getProductId() == null ? null : travelProductMapper.selectById(order.getProductId());
        User user = userMapper.selectById(userId);
        User provider = userMapper.selectById(order.getProviderId());

        OrderChangeRequest request = new OrderChangeRequest();
        request.setOrderId(order.getId());
        request.setOrderNo(order.getOrderNo());
        request.setProductTitle(product == null ? null : product.getTitle());
        request.setProviderId(order.getProviderId());
        request.setProviderName(provider == null ? null : provider.getNickname());
        request.setUserId(userId);
        request.setUserName(user == null ? null : user.getNickname());
        request.setExpectedDate(payload.getExpectedDate());
        request.setReason(payload.getReason().trim());
        request.setStatus(Constants.CHANGE_REQUEST_PENDING);
        request.setReviewRemark("");
        request.setCreatedAt(LocalDateTime.now());
        orderChangeRequestMapper.insert(request);
        return request;
    }

    /**
     * 根据订单列表查询变更申请。
     * 游客仅看自己的，服务商仅看自己的订单，管理员可看全部。
     */
    public List<OrderChangeRequest> listByOrderIds(Long actorId, Integer role, List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<OrderChangeRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderChangeRequest::getOrderId, orderIds);
        if (role != null && role == Constants.ROLE_USER) {
            wrapper.eq(OrderChangeRequest::getUserId, actorId);
        } else if (role != null && role == Constants.ROLE_PROVIDER) {
            wrapper.eq(OrderChangeRequest::getProviderId, actorId);
        } else if (role == null || role != Constants.ROLE_ADMIN) {
            throw new BusinessException("当前角色无权查询变更申请");
        }
        wrapper.orderByDesc(OrderChangeRequest::getCreatedAt);
        return orderChangeRequestMapper.selectList(wrapper);
    }

    /**
     * 服务商审核变更申请。
     * 审核通过后，同步更新订单出行日期。
     */
    public OrderChangeRequest review(Long requestId, Long providerId, Integer status, String reviewRemark) {
        if (status == null
                || (status != Constants.CHANGE_REQUEST_APPROVED && status != Constants.CHANGE_REQUEST_REJECTED)) {
            throw new BusinessException("审核状态必须为 1(通过) 或 2(拒绝)");
        }

        OrderChangeRequest request = orderChangeRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("变更申请不存在");
        }
        if (!providerId.equals(request.getProviderId())) {
            throw new BusinessException("无权审核该申请");
        }
        if (request.getStatus() == null || request.getStatus() != Constants.CHANGE_REQUEST_PENDING) {
            throw new BusinessException("该申请已处理，不能重复审核");
        }

        request.setStatus(status);
        request.setReviewRemark(reviewRemark == null ? "" : reviewRemark.trim());
        request.setReviewedAt(LocalDateTime.now());
        orderChangeRequestMapper.updateById(request);

        if (status == Constants.CHANGE_REQUEST_APPROVED && request.getExpectedDate() != null) {
            TravelOrder order = travelOrderMapper.selectById(request.getOrderId());
            if (order != null && providerId.equals(order.getProviderId())) {
                order.setTravelDate(request.getExpectedDate());
                travelOrderMapper.updateById(order);
            }
        }

        return request;
    }
}
