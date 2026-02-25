package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transport")
public class Transport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer type;
    private String departure;
    private String arrival;
    private BigDecimal price;
    private String description;
    private Long providerId;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
