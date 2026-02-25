package com.travel.common;

public class Constants {
    // 角色
    public static final int ROLE_ADMIN = 0;
    public static final int ROLE_USER = 1;
    public static final int ROLE_PROVIDER = 2;

    // 账号状态
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    // 审核状态
    public static final int AUDIT_PENDING = 0;
    public static final int AUDIT_APPROVED = 1;
    public static final int AUDIT_REJECTED = 2;

    // 订单状态
    public static final int ORDER_UNPAID = 0;
    public static final int ORDER_PAID = 1;
    public static final int ORDER_IN_PROGRESS = 2;
    public static final int ORDER_COMPLETED = 3;
    public static final int ORDER_CANCELLED = 4;
    public static final int ORDER_REFUNDING = 5;
    public static final int ORDER_REFUNDED = 6;

    // 用户行为类型
    public static final int BEHAVIOR_VIEW = 0;
    public static final int BEHAVIOR_FAVORITE = 1;
    public static final int BEHAVIOR_PURCHASE = 2;
    public static final int BEHAVIOR_RATE = 3;

    // 行为权重（用于协同过滤）
    public static final double WEIGHT_VIEW = 1.0;
    public static final double WEIGHT_FAVORITE = 3.0;
    public static final double WEIGHT_PURCHASE = 5.0;
}
