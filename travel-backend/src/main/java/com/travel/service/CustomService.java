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

/**
 * 类说明：CustomService
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@Service
@RequiredArgsConstructor
public class CustomService {

    private final CustomRequestMapper requestMapper;
    private final CustomPlanMapper planMapper;
    private final UserMapper userMapper;
    private final DestinationMapper destinationMapper;

    /**
     * 方法说明：submitRequest
     * 1. 负责处理 submitRequest 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void submitRequest(CustomRequest request) {
        request.setStatus(0);
        requestMapper.insert(request);
    }

    /**
     * 方法说明：userRequests
     * 1. 负责处理 userRequests 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public PageResult<CustomRequest> userRequests(Long userId, Integer page, Integer size) {
        Page<CustomRequest> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CustomRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomRequest::getUserId, userId);
        wrapper.orderByDesc(CustomRequest::getCreateTime);
        Page<CustomRequest> result = requestMapper.selectPage(pageParam, wrapper);
        fillRequestInfo(result);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 方法说明：providerRequests
     * 1. 负责处理 providerRequests 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
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

    /**
     * 方法说明：getRequestById
     * 1. 负责处理 getRequestById 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public CustomRequest getRequestById(Long id) {
        return requestMapper.selectById(id);
    }

    /**
     * 方法说明：getRequestByIdForUser
     * 1. 负责处理 getRequestByIdForUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public CustomRequest getRequestByIdForUser(Long id, Long userId) {
        CustomRequest request = requestMapper.selectById(id);
        if (request == null || !userId.equals(request.getUserId())) {
            throw new BusinessException("request not found");
        }
        return request;
    }

    /**
     * 方法说明：submitPlan
     * 1. 负责处理 submitPlan 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
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

    /**
     * 方法说明：getPlanByRequestId
     * 1. 负责处理 getPlanByRequestId 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public CustomPlan getPlanByRequestId(Long requestId) {
        return planMapper.selectOne(
                new LambdaQueryWrapper<CustomPlan>()
                        .eq(CustomPlan::getRequestId, requestId)
                        .orderByDesc(CustomPlan::getCreateTime)
                        .last("LIMIT 1"));
    }

    /**
     * 方法说明：getPlanByRequestIdForUser
     * 1. 负责处理 getPlanByRequestIdForUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public CustomPlan getPlanByRequestIdForUser(Long requestId, Long userId) {
        CustomRequest request = getRequestByIdForUser(requestId, userId);
        return getPlanByRequestId(request.getId());
    }

    /**
     * 方法说明：acceptPlan
     * 1. 负责处理 acceptPlan 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
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
     * 方法说明：rejectPlan
     * 1. 负责处理 rejectPlan 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
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
     * 方法说明：fillRequestInfo
     * 1. 负责处理 fillRequestInfo 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
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
