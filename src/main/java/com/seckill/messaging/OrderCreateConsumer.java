package com.seckill.messaging;

import com.seckill.service.OrderService;
import com.seckill.service.support.OrderProgressService;
import com.seckill.service.support.SeckillReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "seckill.kafka.enabled", havingValue = "true")
public class OrderCreateConsumer {

    private final OrderService orderService;
    private final SeckillReservationService seckillReservationService;
    private final OrderProgressService orderProgressService;

    @KafkaListener(
            topics = "${seckill.kafka.topics.order-create}",
            groupId = "${seckill.kafka.groups.order-create}"
    )
    public void consume(SeckillOrderCreateMessage message) {
        OrderService.OrderCreateResult result = orderService.createOrderFromMessage(message);
        switch (result) {
            case SUCCESS -> {
                orderProgressService.clear(message.getOrderId());
                log.info("订单创建成功: orderId={}, userId={}, productId={}",
                        message.getOrderId(), message.getUserId(), message.getProductId());
            }
            case DUPLICATE -> {
                orderProgressService.markFailed(message.getOrderId(), message.getUserId(), "重复下单，请勿重复提交");
                log.info("幂等命中，重复下单请求被忽略: userId={}, productId={}",
                        message.getUserId(), message.getProductId());
            }
            case SOLD_OUT, PRODUCT_NOT_FOUND -> {
                seckillReservationService.release(message.getProductId(), message.getUserId());
                orderProgressService.markFailed(message.getOrderId(), message.getUserId(), "库存不足或商品不存在");
                log.warn("订单创建失败并已回补Redis库存: orderId={}, result={}", message.getOrderId(), result);
            }
            default -> throw new IllegalStateException("未知订单创建结果: " + result);
        }
    }
}
