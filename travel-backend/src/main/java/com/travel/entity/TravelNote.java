package com.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 游记实体。
 */
@Data
@TableName("travel_note")
public class TravelNote {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布用户 ID */
    private Long userId;

    /** 关联订单 ID（可为空） */
    private Long orderId;

    private String title;
    private String destinationName;
    private LocalDate travelDate;
    private String content;

    /** JSON 字符串数组 */
    private String tags;

    /** JSON 字符串数组 */
    private String images;

    private Integer rating;
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createdAt;

    private LocalDateTime updateTime;

    /** 以下字段不落库，用于前端展示 */
    @TableField(exist = false)
    private Long authorId;

    @TableField(exist = false)
    private String authorName;
}
