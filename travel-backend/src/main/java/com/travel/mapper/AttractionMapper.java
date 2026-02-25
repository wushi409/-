package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.Attraction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttractionMapper extends BaseMapper<Attraction> {
}
