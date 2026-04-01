package com.seckill.messaging;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SeckillOrderCreateMessage implements Serializable {
    private String messageId;
    private Long orderId;
    private Long userId;
    private Long productId;
    private LocalDateTime createTime;
}

