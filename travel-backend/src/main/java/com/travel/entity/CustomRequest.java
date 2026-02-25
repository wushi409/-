package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("custom_request")
public class CustomRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long providerId;
    private Long destinationId;
    private String title;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer peopleCount;
    private String preferences;
    private String interestTags;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String destinationName;
}
