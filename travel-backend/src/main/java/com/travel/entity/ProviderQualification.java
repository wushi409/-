package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("provider_qualification")
public class ProviderQualification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String companyName;
    private String licenseNo;
    private String licenseImage;
    private String contactPerson;
    private String contactPhone;
    private Integer auditStatus;
    private String auditRemark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
