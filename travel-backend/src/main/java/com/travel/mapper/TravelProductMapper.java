package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.TravelProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TravelProductMapper extends BaseMapper<TravelProduct> {
}
