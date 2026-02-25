package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.entity.Transport;
import com.travel.mapper.TransportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 类说明：TransportService
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@Service
@RequiredArgsConstructor
public class TransportService {

    private final TransportMapper transportMapper;

    /**
     * 方法说明：list
     * 1. 负责处理 list 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public PageResult<Transport> list(Integer page, Integer size, Integer type, String departure, String arrival) {
        Page<Transport> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Transport> wrapper = new LambdaQueryWrapper<>();
        
        if (type != null) {
            wrapper.eq(Transport::getType, type);
        }
        if (departure != null && !departure.isEmpty()) {
            wrapper.like(Transport::getDeparture, departure);
        }
        if (arrival != null && !arrival.isEmpty()) {
            wrapper.like(Transport::getArrival, arrival);
        }
        wrapper.orderByDesc(Transport::getCreateTime);
        
        Page<Transport> result = transportMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 方法说明：add
     * 1. 负责处理 add 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void add(Transport transport) {
        transportMapper.insert(transport);
    }

    /**
     * 方法说明：update
     * 1. 负责处理 update 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void update(Transport transport) {
        transportMapper.updateById(transport);
    }

    /**
     * 方法说明：delete
     * 1. 负责处理 delete 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void delete(Long id) {
        transportMapper.deleteById(id);
    }

    /**
     * 方法说明：getById
     * 1. 负责处理 getById 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public Transport getById(Long id) {
        return transportMapper.selectById(id);
    }
}
