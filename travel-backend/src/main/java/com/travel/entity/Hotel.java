package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("hotel")
public class Hotel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long destinationId;
    private Long providerId;
    private String name;
    private Integer starLevel;
    private String description;
    private String coverImage;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
