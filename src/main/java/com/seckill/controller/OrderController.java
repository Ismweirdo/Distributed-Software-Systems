package com.seckill.controller;

import com.seckill.entity.SeckillOrder;
import com.seckill.service.OrderService;
import com.seckill.vo.Result;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Validated
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public Result<SeckillOrder> getByOrderId(@PathVariable @NotNull @Min(1) Long orderId) {
        return orderService.getByOrderId(orderId);
    }

    @GetMapping("/user")
    public Result<List<SeckillOrder>> listByUserId(@RequestParam @NotNull @Min(1) Long userId) {
        return orderService.listByUserId(userId);
    }
}

