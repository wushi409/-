/**
 * ChatMessageMapper Mapper。
 * 负责数据库读写，对应 SQL 条件在这里找。
 */
package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
