package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("travel_order")
public class TravelOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long providerId;
    private Long productId;
    private Long customPlanId;
    private Integer orderType;
    private BigDecimal totalAmount;
    private Integer status;
    private Integer peopleCount;
    private LocalDate travelDate;
    private String contactName;
    private String contactPhone;
    private String remark;
    private LocalDateTime payTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String productTitle;
    @TableField(exist = false)
    private String providerName;
}
