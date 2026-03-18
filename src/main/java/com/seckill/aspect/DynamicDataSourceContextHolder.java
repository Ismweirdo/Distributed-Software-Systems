package com.seckill.aspect;

import com.baomidou.dynamic.datasource.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;

/**
 * 动态数据源上下文持有器
 */
@Slf4j
public class DynamicDataSourceContextHolder {

    /**
     * 设置数据源
     * @param dataSourceName 数据源名称 (master, slave_1, slave_2)
     */
    public static void setDataSource(String dataSourceName) {
        log.debug("切换数据源到：{}", dataSourceName);
        DynamicDataSourceContextHolder.push(dataSourceName);
    }

    /**
     * 获取当前数据源
     */
    public static String getDataSource() {
        return DynamicDataSourceContextHolder.peek();
    }

    /**
     * 清除数据源
     */
    public static void clearDataSource() {
        log.debug("清除数据源上下文");
        DynamicDataSourceContextHolder.poll();
    }
}
