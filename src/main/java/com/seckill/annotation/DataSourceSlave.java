package com.seckill.annotation;

import java.lang.annotation.*;

/**
 * 从库读取注解
 * 标注在 Service 方法上，表示使用从库（读库）查询
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSourceSlave {
}
