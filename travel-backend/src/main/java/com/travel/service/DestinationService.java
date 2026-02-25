package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.entity.Destination;
import com.travel.mapper.DestinationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationMapper destinationMapper;

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

    public Destination getById(Long id) {
        return destinationMapper.selectById(id);
    }

    public void add(Destination destination) {
        destination.setStatus(1);
        destination.setHotScore(0);
        destinationMapper.insert(destination);
    }

    public void update(Destination destination) {
        destinationMapper.updateById(destination);
    }

    public void delete(Long id) {
        destinationMapper.deleteById(id);
    }

    public List<Destination> hotList(int limit) {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Destination::getStatus, 1);
        wrapper.orderByDesc(Destination::getHotScore);
        wrapper.last("LIMIT " + limit);
        return destinationMapper.selectList(wrapper);
    }
}
