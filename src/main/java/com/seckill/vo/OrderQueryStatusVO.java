package com.seckill.vo;

import com.seckill.entity.SeckillOrder;
import lombok.Data;

@Data
public class OrderQueryStatusVO {
    public static final String CREATED = "CREATED";
    public static final String PENDING = "PENDING";
    public static final String FAILED = "FAILED";
    public static final String NOT_FOUND = "NOT_FOUND";

    private String status;
    private String message;
    private SeckillOrder order;

    public static OrderQueryStatusVO created(SeckillOrder order) {
        OrderQueryStatusVO vo = new OrderQueryStatusVO();
        vo.setStatus(CREATED);
        vo.setMessage("订单已创建");
        vo.setOrder(order);
        return vo;
    }

    public static OrderQueryStatusVO pending(String message) {
        OrderQueryStatusVO vo = new OrderQueryStatusVO();
        vo.setStatus(PENDING);
        vo.setMessage(message);
        return vo;
    }

    public static OrderQueryStatusVO failed(String message) {
        OrderQueryStatusVO vo = new OrderQueryStatusVO();
        vo.setStatus(FAILED);
        vo.setMessage(message);
        return vo;
    }

    public static OrderQueryStatusVO notFound(String message) {
        OrderQueryStatusVO vo = new OrderQueryStatusVO();
        vo.setStatus(NOT_FOUND);
        vo.setMessage(message);
        return vo;
    }
}

