package com.seckill.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.seckill.vo.OrderQueryStatusVO;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OrderProgressService {

    private static final String ORDER_PROGRESS_KEY_PREFIX = "seckill:order:progress:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${seckill.order.pending-ttl-seconds:60}")
    private long pendingTtlSeconds;

    @Value("${seckill.order.failed-ttl-seconds:120}")
    private long failedTtlSeconds;

    public void markPending(Long orderId, Long userId) {
        stringRedisTemplate.opsForValue().set(
                buildKey(orderId),
                OrderQueryStatusVO.PENDING + "|" + userId,
                pendingTtlSeconds,
                TimeUnit.SECONDS
        );
    }

    public void markFailed(Long orderId, Long userId, String reason) {
        String message = reason == null || reason.isBlank() ? "下单失败" : reason;
        stringRedisTemplate.opsForValue().set(
                buildKey(orderId),
                OrderQueryStatusVO.FAILED + "|" + userId + "|" + message,
                failedTtlSeconds,
                TimeUnit.SECONDS
        );
    }

    public void clear(Long orderId) {
        stringRedisTemplate.delete(buildKey(orderId));
    }

    public ProgressState getState(Long orderId) {
        String value = stringRedisTemplate.opsForValue().get(buildKey(orderId));
        if (value == null || value.isBlank()) {
            return ProgressState.notFound();
        }
        String pendingPrefix = OrderQueryStatusVO.PENDING + "|";
        if (value.startsWith(pendingPrefix)) {
            Long ownerId = parseLongSafe(value.substring(pendingPrefix.length()));
            return ProgressState.pending(ownerId);
        }
        String failedPrefix = OrderQueryStatusVO.FAILED + "|";
        if (value.startsWith(failedPrefix)) {
            String[] parts = value.split("\\|", 3);
            if (parts.length == 3) {
                return ProgressState.failed(parseLongSafe(parts[1]), parts[2]);
            }
        }
        return ProgressState.notFound();
    }

    private String buildKey(Long orderId) {
        return ORDER_PROGRESS_KEY_PREFIX + orderId;
    }

    public record ProgressState(String status, String message, Long ownerUserId) {
        public static ProgressState pending(Long ownerUserId) {
            return new ProgressState(OrderQueryStatusVO.PENDING, "下单处理中", ownerUserId);
        }

        public static ProgressState failed(Long ownerUserId, String message) {
            return new ProgressState(OrderQueryStatusVO.FAILED, message == null || message.isBlank() ? "下单失败" : message, ownerUserId);
        }

        public static ProgressState notFound() {
            return new ProgressState(OrderQueryStatusVO.NOT_FOUND, "未找到订单状态", null);
        }
    }

    private Long parseLongSafe(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception exception) {
            return null;
        }
    }
}

