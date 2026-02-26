package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.OrderChangeRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行程变更申请表 Mapper。
 */
@Mapper
public interface OrderChangeRequestMapper extends BaseMapper<OrderChangeRequest> {
}
