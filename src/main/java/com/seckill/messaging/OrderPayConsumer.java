package com.seckill.messaging;

import com.seckill.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "seckill.kafka.enabled", havingValue = "true")
public class OrderPayConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${seckill.kafka.topics.order-pay}",
            groupId = "${seckill.kafka.groups.order-pay}"
    )
    public void consume(SeckillOrderPayMessage message) {
        OrderService.PayResult result = orderService.confirmOrderPayment(message);
        log.info("支付消息处理完成: orderId={}, result={}", message.getOrderId(), result);
    }
}

