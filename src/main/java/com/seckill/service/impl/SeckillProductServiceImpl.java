package com.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.annotation.DataSourceSlave;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.messaging.OrderCreateProducer;
import com.seckill.messaging.SeckillOrderCreateMessage;
import com.seckill.service.CacheService;
import com.seckill.service.SeckillProductService;
import com.seckill.service.support.SeckillReservationService;
import com.seckill.util.SnowflakeIdGenerator;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct>
        implements SeckillProductService {

    private final CacheService cacheService;
    private final SeckillReservationService seckillReservationService;
    private final OrderCreateProducer orderCreateProducer;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @DataSourceSlave
    public Result<List<SeckillProductVO>> listProducts() {
        List<SeckillProductVO> productVOList = list().stream()
                .map(this::convertToVO)
                .toList();
        return Result.success(productVOList);
    }

    @Override
    @DataSourceSlave
    public Result<SeckillProductVO> getProductById(Long id) {
        SeckillProductVO product = cacheService.getProductFromCache(id);
        if (product == null) {
            return Result.fail("商品不存在或已被删除");
        }
        return Result.success(product);
    }

    @Override
    public Result<Boolean> seckillProduct(Long productId) {
        Result<Long> asyncResult = seckillProduct(productId, 1L);
        if (asyncResult.getCode() != 200) {
            return Result.fail(asyncResult.getMsg());
        }
        return Result.success(true);
    }

    @Override
    public Result<Long> seckillProduct(Long productId, Long userId) {
        SeckillProduct product = getById(productId);
        if (product == null) {
            return Result.fail("商品不存在");
        }

        seckillReservationService.initializeStockIfAbsent(productId, product.getStock());
        SeckillReservationService.ReserveResult reserveResult = seckillReservationService.reserve(productId, userId);
        if (reserveResult == SeckillReservationService.ReserveResult.DUPLICATE) {
            return Result.fail("请勿重复下单");
        }
        if (reserveResult == SeckillReservationService.ReserveResult.OUT_OF_STOCK) {
            return Result.fail("库存不足");
        }
        if (reserveResult == SeckillReservationService.ReserveResult.STOCK_NOT_INITIALIZED
                || reserveResult == SeckillReservationService.ReserveResult.ERROR) {
            return Result.fail("系统繁忙，请稍后重试");
        }

        Long orderId = snowflakeIdGenerator.nextId();
        SeckillOrderCreateMessage message = new SeckillOrderCreateMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setProductId(productId);
        message.setCreateTime(LocalDateTime.now());

        boolean sent = orderCreateProducer.send(message);
        if (!sent) {
            seckillReservationService.release(productId, userId);
            return Result.fail("下单请求提交失败，请重试");
        }

        cacheService.deleteProductCache(productId);
        log.info("秒杀请求已入队: orderId={}, userId={}, productId={}", orderId, userId, productId);
        return Result.success(orderId);
    }

    private SeckillProductVO convertToVO(SeckillProduct product) {
        SeckillProductVO vo = new SeckillProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
}
