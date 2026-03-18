package com.seckill.aspect;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 根据 @DataSourceSlave 注解在读请求前切换到从库，并在结束后清理上下文。
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class DataSourceAspect {

    private static final int SLAVE_COUNT = 2;

    @Around("@annotation(com.seckill.annotation.DataSourceSlave)")
    public Object switchToSlaveAndRestore(ProceedingJoinPoint joinPoint) throws Throwable {
        String slaveName = selectSlave();
        DynamicDataSourceContextHolder.push(slaveName);
        log.debug("切换数据源到: {}", slaveName);

        try {
            return joinPoint.proceed();
        } finally {
            DynamicDataSourceContextHolder.poll();
            log.debug("清理数据源上下文: {}", slaveName);
        }
    }

    private String selectSlave() {
        int slaveIndex = ThreadLocalRandom.current().nextInt(1, SLAVE_COUNT + 1);
        return "slave_" + slaveIndex;
    }
}
