package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.entity.Destination;
import com.travel.mapper.DestinationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类说明：DestinationService
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationMapper destinationMapper;

    /**
     * 方法说明：list
     * 1. 负责处理 list 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public PageResult<Destination> list(Integer page, Integer size, String keyword) {
        Page<Destination> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Destination::getName, keyword)
                    .or().like(Destination::getProvince, keyword)
                    .or().like(Destination::getCity, keyword));
        }
        wrapper.eq(Destination::getStatus, 1);
        wrapper.orderByDesc(Destination::getHotScore);
        Page<Destination> result = destinationMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 方法说明：adminList
     * 1. 负责处理 adminList 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public PageResult<Destination> adminList(Integer page, Integer size, String keyword) {
        Page<Destination> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Destination::getName, keyword);
        }
        wrapper.orderByDesc(Destination::getCreateTime);
        Page<Destination> result = destinationMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 方法说明：getById
     * 1. 负责处理 getById 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public Destination getById(Long id) {
        return destinationMapper.selectById(id);
    }

    /**
     * 方法说明：add
     * 1. 负责处理 add 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void add(Destination destination) {
        destination.setStatus(1);
        destination.setHotScore(0);
        destinationMapper.insert(destination);
    }

    /**
     * 方法说明：update
     * 1. 负责处理 update 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void update(Destination destination) {
        destinationMapper.updateById(destination);
    }

    /**
     * 方法说明：delete
     * 1. 负责处理 delete 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void delete(Long id) {
        destinationMapper.deleteById(id);
    }

    /**
     * 方法说明：hotList
     * 1. 负责处理 hotList 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public List<Destination> hotList(int limit) {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Destination::getStatus, 1);
        wrapper.orderByDesc(Destination::getHotScore);
        wrapper.last("LIMIT " + limit);
        return destinationMapper.selectList(wrapper);
    }
}
