package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("attraction")
public class Attraction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long destinationId;
    private String name;
    private String description;
    private String coverImage;
    private BigDecimal ticketPrice;
    private String openTime;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String tags;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
