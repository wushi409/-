package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.entity.Hotel;
import com.travel.mapper.HotelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 类说明：HotelService
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelMapper hotelMapper;

    /**
     * 方法说明：list
     * 1. 负责处理 list 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public PageResult<Hotel> list(Integer page, Integer size, Long destinationId, String keyword) {
        Page<Hotel> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Hotel> wrapper = new LambdaQueryWrapper<>();
        if (destinationId != null) {
            wrapper.eq(Hotel::getDestinationId, destinationId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Hotel::getName, keyword);
        }
        wrapper.eq(Hotel::getStatus, 1);
        wrapper.orderByDesc(Hotel::getCreateTime);
        Page<Hotel> result = hotelMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 方法说明：getById
     * 1. 负责处理 getById 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public Hotel getById(Long id) {
        return hotelMapper.selectById(id);
    }

    /**
     * 方法说明：add
     * 1. 负责处理 add 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void add(Hotel hotel) {
        hotel.setStatus(1);
        hotelMapper.insert(hotel);
    }

    /**
     * 方法说明：update
     * 1. 负责处理 update 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void update(Hotel hotel) {
        hotelMapper.updateById(hotel);
    }

    /**
     * 方法说明：delete
     * 1. 负责处理 delete 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void delete(Long id) {
        hotelMapper.deleteById(id);
    }
}
