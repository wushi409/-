package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("destination")
public class Destination {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String province;
    private String city;
    private String description;
    private String coverImage;
    private String images;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer hotScore;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
