package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.PageResult;
import com.travel.entity.Attraction;
import com.travel.mapper.AttractionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttractionService {

    private final AttractionMapper attractionMapper;

    public PageResult<Attraction> list(Integer page, Integer size, Long destinationId, String keyword) {
        Page<Attraction> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        if (destinationId != null) {
            wrapper.eq(Attraction::getDestinationId, destinationId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Attraction::getName, keyword);
        }
        wrapper.eq(Attraction::getStatus, 1);
        wrapper.orderByDesc(Attraction::getCreateTime);
        Page<Attraction> result = attractionMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Attraction getById(Long id) {
        return attractionMapper.selectById(id);
    }

    public void add(Attraction attraction) {
        attraction.setStatus(1);
        attractionMapper.insert(attraction);
    }

    public void update(Attraction attraction) {
        attractionMapper.updateById(attraction);
    }

    public void delete(Long id) {
        attractionMapper.deleteById(id);
    }
}
