package com.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单行程变更申请实体。
 */
@Data
@TableName("order_change_request")
public class OrderChangeRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private String orderNo;
    private String productTitle;

    private Long providerId;
    private String providerName;

    private Long userId;
    private String userName;

    /** 用户希望改期到的日期 */
    private LocalDate expectedDate;

    /** 申请原因 */
    private String reason;

    /** 0-待处理 1-通过 2-拒绝 */
    private Integer status;

    /** 服务商审核说明 */
    private String reviewRemark;

    @TableField("create_time")
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
