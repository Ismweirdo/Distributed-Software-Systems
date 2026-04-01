package com.seckill.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.service.support.SeckillReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillStockInitializer {

    private final SeckillProductMapper seckillProductMapper;
    private final SeckillReservationService seckillReservationService;

    @EventListener(ApplicationReadyEvent.class)
    public void loadStockToRedis() {
        List<SeckillProduct> products = seckillProductMapper.selectList(
                Wrappers.<SeckillProduct>query().select("id", "stock")
        );
        for (SeckillProduct product : products) {
            if (product.getId() != null && product.getStock() != null) {
                seckillReservationService.initializeStockIfAbsent(product.getId(), product.getStock());
            }
        }
        log.info("Redis库存预热完成: products={}", products.size());
    }
}

