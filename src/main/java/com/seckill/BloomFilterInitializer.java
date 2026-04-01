package com.seckill;

import com.seckill.mapper.SeckillProductMapper;
import com.seckill.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时把所有商品 ID 加入布隆过滤器，防止缓存穿透
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterInitializer {

    private final SeckillProductMapper productMapper;
    private final CacheService cacheService;

    @EventListener(ApplicationReadyEvent.class)
    public void initBloomFilter() {
        try {
            List<Long> ids = productMapper.selectAllIds();
            if (ids == null || ids.isEmpty()) {
                log.info("没有商品需要加载到布隆过滤器");
                return;
            }
            ids.forEach(cacheService::addToBloomFilter);
            log.info("布隆过滤器初始化完成，共加载商品数量：{}", ids.size());
        } catch (Exception e) {
            log.warn("布隆过滤器初始化异常：{}", e.getMessage(), e);
        }
    }
}
