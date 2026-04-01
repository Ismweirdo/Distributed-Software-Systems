package com.seckill.messaging;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillOrderPayMessage implements Serializable {
    private String messageId;
    private Long orderId;
    private Long userId;
    private BigDecimal payAmount;
    private LocalDateTime payTime;
}

