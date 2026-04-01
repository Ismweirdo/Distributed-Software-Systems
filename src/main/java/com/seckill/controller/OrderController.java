package com.seckill.controller;

import com.seckill.entity.SeckillOrder;
import com.seckill.service.OrderService;
import com.seckill.vo.OrderQueryStatusVO;
import com.seckill.vo.Result;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Validated
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public Result<OrderQueryStatusVO> getByOrderId(
            @PathVariable @NotNull @Min(1) Long orderId,
            @RequestHeader(value = "X-Login-Token", required = false) String loginToken) {
        Long userId = resolveUserId(loginToken);
        if (userId == null) {
            return Result.fail("未登录或登录已失效");
        }
        return orderService.queryOrderStatus(orderId, userId);
    }

    @GetMapping("/user")
    public Result<List<SeckillOrder>> listByUserId(
            @RequestHeader(value = "X-Login-Token", required = false) String loginToken) {
        Long userId = resolveUserId(loginToken);
        if (userId == null) {
            return Result.fail("未登录或登录已失效");
        }
        return orderService.listByUserId(userId);
    }

    @PostMapping("/{orderId}/pay")
    public Result<String> payOrder(
            @PathVariable @NotNull @Min(1) Long orderId,
            @RequestHeader(value = "X-Login-Token", required = false) String loginToken) {
        Long userId = resolveUserId(loginToken);
        if (userId == null) {
            return Result.fail("未登录或登录已失效");
        }
        return orderService.payOrder(orderId, userId);
    }

    private Long resolveUserId(String loginToken) {
        if (loginToken == null || !loginToken.startsWith("login_success_")) {
            return null;
        }
        try {
            return Long.parseLong(loginToken.substring("login_success_".length()));
        } catch (Exception exception) {
            return null;
        }
    }
}
