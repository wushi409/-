package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.TravelNote;
import org.apache.ibatis.annotations.Mapper;

/**
 * 游记表 Mapper。
 */
@Mapper
public interface TravelNoteMapper extends BaseMapper<TravelNote> {
}
