package com.seckill.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateProducer {

    private final KafkaTemplate<String, SeckillOrderCreateMessage> kafkaTemplate;

    @Value("${seckill.kafka.topics.order-create}")
    private String orderCreateTopic;

    @Value("${seckill.kafka.send-timeout-ms:1500}")
    private long sendTimeoutMs;

    public boolean send(SeckillOrderCreateMessage message) {
        try {
            SendResult<String, SeckillOrderCreateMessage> result = kafkaTemplate
                    .send(orderCreateTopic, String.valueOf(message.getOrderId()), message)
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("发送下单消息成功: orderId={}, topic={}, partition={}, offset={}",
                    message.getOrderId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;
        } catch (Exception e) {
            log.error("发送下单消息失败: orderId={}", message.getOrderId(), e);
            return false;
        }
    }
}

