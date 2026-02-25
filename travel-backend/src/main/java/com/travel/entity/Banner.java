package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("banner")
public class Banner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
