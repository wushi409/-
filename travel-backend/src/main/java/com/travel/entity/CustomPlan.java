package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("custom_plan")
public class CustomPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requestId;
    private Long providerId;
    private String title;
    private String description;
    private BigDecimal totalPrice;
    private String dayPlans;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
