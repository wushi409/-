package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("travel_product")
public class TravelProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long providerId;
    private String title;
    private String description;
    private String coverImage;
    private String images;
    private Long destinationId;
    private Integer duration;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer productType;
    private String tags;
    private String includeItems;
    private String excludeItems;
    private Integer stock;
    private Integer sales;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 非数据库字段
    @TableField(exist = false)
    private String providerName;
    @TableField(exist = false)
    private String destinationName;
}
