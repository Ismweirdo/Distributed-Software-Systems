package com.seckill.aspect;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Aspect
@Order(1)
@Component
@ConditionalOnProperty(name = "seckill.datasource.mode", havingValue = "dynamic", matchIfMissing = true)
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
