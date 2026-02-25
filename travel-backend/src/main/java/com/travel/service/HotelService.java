package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.entity.Hotel;
import com.travel.mapper.HotelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelMapper hotelMapper;

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

    public Hotel getById(Long id) {
        return hotelMapper.selectById(id);
    }

    public void add(Hotel hotel) {
        hotel.setStatus(1);
        hotelMapper.insert(hotel);
    }

    public void update(Hotel hotel) {
        hotelMapper.updateById(hotel);
    }

    public void delete(Long id) {
        hotelMapper.deleteById(id);
    }
}
