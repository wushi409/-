package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("travel_day_plan")
public class TravelDayPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Integer dayNumber;
    private String title;
    private String description;
    private String attractionIds;
    private Long hotelId;
    private Long transportId;
}
