package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.entity.SeckillOrder;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.messaging.SeckillOrderCreateMessage;
import com.seckill.service.OrderService;
import com.seckill.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements OrderService {

    private final SeckillProductMapper seckillProductMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResult createOrderFromMessage(SeckillOrderCreateMessage message) {
        SeckillOrder existingOrder = lambdaQuery()
                .eq(SeckillOrder::getUserId, message.getUserId())
                .eq(SeckillOrder::getProductId, message.getProductId())
                .one();
        if (existingOrder != null) {
            return OrderCreateResult.DUPLICATE;
        }

        SeckillProduct product = seckillProductMapper.selectById(message.getProductId());
        if (product == null) {
            return OrderCreateResult.PRODUCT_NOT_FOUND;
        }

        int updated = seckillProductMapper.decreaseStock(message.getProductId());
        if (updated <= 0) {
            return OrderCreateResult.SOLD_OUT;
        }

        SeckillOrder order = new SeckillOrder();
        LocalDateTime now = LocalDateTime.now();
        order.setOrderId(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setOrderAmount(product.getSeckillPrice());
        order.setOrderStatus("CREATED");
        order.setCreateTime(now);
        order.setUpdateTime(now);

        try {
            save(order);
        } catch (DuplicateKeyException duplicateKeyException) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return OrderCreateResult.DUPLICATE;
        }
        return OrderCreateResult.SUCCESS;
    }

    @Override
    public Result<SeckillOrder> getByOrderId(Long orderId) {
        SeckillOrder order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        return Result.success(order);
    }

    @Override
    public Result<List<SeckillOrder>> listByUserId(Long userId) {
        List<SeckillOrder> orders = list(Wrappers.<SeckillOrder>lambdaQuery()
                .eq(SeckillOrder::getUserId, userId)
                .orderByDesc(SeckillOrder::getCreateTime));
        return Result.success(orders);
    }
}
