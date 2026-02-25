/**
 * 定制需求业务：游客提需求、服务商出方案、游客确认结果。
 */
package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.common.exception.BusinessException;
import com.travel.entity.CustomPlan;
import com.travel.entity.CustomRequest;
import com.travel.entity.Destination;
import com.travel.entity.User;
import com.travel.mapper.CustomPlanMapper;
import com.travel.mapper.CustomRequestMapper;
import com.travel.mapper.DestinationMapper;
import com.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomService {

    private final CustomRequestMapper requestMapper;
    private final CustomPlanMapper planMapper;
    private final UserMapper userMapper;
    private final DestinationMapper destinationMapper;

    /**
     * 游客提交定制需求（状态=0：待处理）。
     * 这是定制流程的入口。
     */
    public void submitRequest(CustomRequest request) {
        request.setStatus(0);
        requestMapper.insert(request);
    }

    public PageResult<CustomRequest> userRequests(Long userId, Integer page, Integer size) {
        Page<CustomRequest> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CustomRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomRequest::getUserId, userId);
        wrapper.orderByDesc(CustomRequest::getCreateTime);
        Page<CustomRequest> result = requestMapper.selectPage(pageParam, wrapper);
        fillRequestInfo(result);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<CustomRequest> providerRequests(Long providerId, Integer page, Integer size) {
        Page<CustomRequest> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CustomRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(CustomRequest::getProviderId, providerId)
                .or().isNull(CustomRequest::getProviderId));
        wrapper.eq(CustomRequest::getStatus, 0);
        wrapper.orderByDesc(CustomRequest::getCreateTime);
        Page<CustomRequest> result = requestMapper.selectPage(pageParam, wrapper);
        fillRequestInfo(result);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public CustomRequest getRequestById(Long id) {
        return requestMapper.selectById(id);
    }

    public CustomRequest getRequestByIdForUser(Long id, Long userId) {
        CustomRequest request = requestMapper.selectById(id);
        if (request == null || !userId.equals(request.getUserId())) {
            throw new BusinessException("request not found");
        }
        return request;
    }

    /**
     * 服务商提交方案主链路。
     *
     * 状态联动：
     * 1. 方案表新增一条记录（status=0：待用户确认）；
     * 2. 需求表同步改为 status=1（已出方案，等待用户处理）。
     */
    public void submitPlan(CustomPlan plan) {
        plan.setStatus(0);
        planMapper.insert(plan);

        CustomRequest request = requestMapper.selectById(plan.getRequestId());
        if (request != null) {
            request.setStatus(1);
            requestMapper.updateById(request);
        }
    }

    public CustomPlan getPlanByRequestId(Long requestId) {
        return planMapper.selectOne(
                new LambdaQueryWrapper<CustomPlan>()
                        .eq(CustomPlan::getRequestId, requestId)
                        .orderByDesc(CustomPlan::getCreateTime)
                        .last("LIMIT 1"));
    }

    public CustomPlan getPlanByRequestIdForUser(Long requestId, Long userId) {
        CustomRequest request = getRequestByIdForUser(requestId, userId);
        return getPlanByRequestId(request.getId());
    }

    /**
     * 游客确认接受方案。
     *
     * 状态联动：
     * 1. 方案 status: 0 -> 1（已接受）；
     * 2. 需求 status: 1 -> 2（已完成确认）。
     */
    public void acceptPlan(Long planId, Long userId) {
        CustomPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("plan not found");
        }

        CustomRequest request = requestMapper.selectById(plan.getRequestId());
        if (request == null || !userId.equals(request.getUserId())) {
            throw new BusinessException("request not found");
        }

        plan.setStatus(1);
        planMapper.updateById(plan);

        request.setStatus(2);
        requestMapper.updateById(request);
    }

    /**
     * 游客拒绝方案。
     * 只改方案状态为 2（已拒绝），需求本身保持原状态，方便继续提交新方案。
     */
    public void rejectPlan(Long planId, Long userId) {
        CustomPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("plan not found");
        }

        CustomRequest request = requestMapper.selectById(plan.getRequestId());
        if (request == null || !userId.equals(request.getUserId())) {
            throw new BusinessException("request not found");
        }

        plan.setStatus(2);
        planMapper.updateById(plan);
    }

    /**
     * 给需求列表补展示字段（用户名、目的地名称）。
     */
    private void fillRequestInfo(Page<CustomRequest> result) {
        result.getRecords().forEach(req -> {
            User user = userMapper.selectById(req.getUserId());
            if (user != null) {
                req.setUserName(user.getNickname());
            }
            if (req.getDestinationId() != null) {
                Destination dest = destinationMapper.selectById(req.getDestinationId());
                if (dest != null) {
                    req.setDestinationName(dest.getName());
                }
            }
        });
    }
}
