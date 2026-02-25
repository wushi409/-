package com.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("product_daily_stock")
public class ProductDailyStock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private LocalDate stockDate;
    private Integer stockTotal;
    private Integer stockAvailable;
    private Integer warnThreshold;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String productTitle;
}

