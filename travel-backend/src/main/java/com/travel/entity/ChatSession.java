package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long providerId;
    private String lastMessage;
    private LocalDateTime lastTime;
    private Integer unreadCount;

    @TableField(exist = false)
    private String otherName;
    @TableField(exist = false)
    private String otherAvatar;
}
