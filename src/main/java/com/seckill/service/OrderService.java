package com.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seckill.entity.SeckillOrder;
import com.seckill.messaging.SeckillOrderCreateMessage;
import com.seckill.messaging.SeckillOrderPayMessage;
import com.seckill.vo.OrderQueryStatusVO;
import com.seckill.vo.Result;

import java.util.List;

public interface OrderService extends IService<SeckillOrder> {

    OrderCreateResult createOrderFromMessage(SeckillOrderCreateMessage message);

    Result<OrderQueryStatusVO> queryOrderStatus(Long orderId, Long currentUserId);

    Result<List<SeckillOrder>> listByUserId(Long userId);

    Result<String> payOrder(Long orderId, Long userId);

    PayResult confirmOrderPayment(SeckillOrderPayMessage message);

    enum OrderCreateResult {
        SUCCESS,
        DUPLICATE,
        SOLD_OUT,
        PRODUCT_NOT_FOUND
    }

    enum PayResult {
        SUCCESS,
        ALREADY_PAID,
        ORDER_NOT_FOUND,
        FORBIDDEN,
        INVALID_STATUS
    }
}
