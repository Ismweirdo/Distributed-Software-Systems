package com.seckill.aspect;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.seckill.annotation.DataSourceSlave;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据源切换切面
 * 根据 @DataSourceSlave 注解动态切换数据源
 */
@Slf4j
@Aspect
@Order(1) // 保证在动态数据源之前执行
@Component
public class DataSourceAspect {

    @Before("@annotation(com.seckill.annotation.DataSourceSlave)")
    public void switchToSlave(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取方法上的注解
        DataSourceSlave annotation = method.getAnnotation(DataSourceSlave.class);
        if (annotation != null) {
            // 随机选择从库（slave_1 或 slave_2）
            String slaveName = "slave_" + ((int)(Math.random() * 2) + 1);
            log.info("切换到从库：{}", slaveName);
            DS ds = method.getAnnotation(DS.class);
            if (ds == null) {
                // 如果没有 DS 注解，手动设置
                DynamicDataSourceContextHolder.setDataSource(slaveName);
            }
        }
    }
}
