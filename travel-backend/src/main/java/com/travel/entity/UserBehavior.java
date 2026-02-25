package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_behavior")
public class UserBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Integer behaviorType;
    private BigDecimal score;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
