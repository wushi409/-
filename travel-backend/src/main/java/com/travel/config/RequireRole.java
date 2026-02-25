/**
 * 自定义权限注解。
 * 在控制器方法上标注角色，拦截器会按这里的角色做鉴权。
 */
package com.travel.config;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    int[] value();
}
