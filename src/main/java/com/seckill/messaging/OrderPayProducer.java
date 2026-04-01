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
public class OrderPayProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${seckill.kafka.topics.order-pay}")
    private String orderPayTopic;

    @Value("${seckill.kafka.send-timeout-ms:1500}")
    private long sendTimeoutMs;

    @Value("${seckill.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public boolean send(SeckillOrderPayMessage message) {
        if (!kafkaEnabled) {
            return false;
        }
        try {
            SendResult<String, Object> result = kafkaTemplate
                    .send(orderPayTopic, String.valueOf(message.getOrderId()), message)
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("发送支付消息成功: orderId={}, partition={}, offset={}",
                    message.getOrderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;
        } catch (Exception exception) {
            log.error("发送支付消息失败: orderId={}", message.getOrderId(), exception);
            return false;
        }
    }
}

