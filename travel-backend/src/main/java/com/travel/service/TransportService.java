package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.entity.Transport;
import com.travel.mapper.TransportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransportService {

    private final TransportMapper transportMapper;

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

    public void add(Transport transport) {
        transportMapper.insert(transport);
    }

    public void update(Transport transport) {
        transportMapper.updateById(transport);
    }

    public void delete(Long id) {
        transportMapper.deleteById(id);
    }

    public Transport getById(Long id) {
        return transportMapper.selectById(id);
    }
}
