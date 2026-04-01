package com.seckill.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SeckillReservationService {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_SET_KEY_PREFIX = "seckill:users:";

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[1]) == 0 then return -3 end " +
                    "if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then return -2 end " +
                    "local stock = tonumber(redis.call('get', KEYS[1])) " +
                    "if (not stock) or stock <= 0 then return -1 end " +
                    "redis.call('decr', KEYS[1]) " +
                    "redis.call('sadd', KEYS[2], ARGV[1]) " +
                    "return stock - 1",
            Long.class
    );

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then " +
                    "redis.call('srem', KEYS[2], ARGV[1]) " +
                    "redis.call('incr', KEYS[1]) " +
                    "return 1 " +
                    "end " +
                    "return 0",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public void initializeStockIfAbsent(Long productId, int dbStock) {
        stringRedisTemplate.opsForValue().setIfAbsent(buildStockKey(productId), String.valueOf(Math.max(dbStock, 0)));
    }

    public ReserveResult reserve(Long productId, Long userId) {
        Long result = stringRedisTemplate.execute(
                RESERVE_SCRIPT,
                List.of(buildStockKey(productId), buildUserSetKey(productId)),
                String.valueOf(userId)
        );
        if (result == null) {
            return ReserveResult.ERROR;
        }
        if (result == -3L) {
            return ReserveResult.STOCK_NOT_INITIALIZED;
        }
        if (result == -2L) {
            return ReserveResult.DUPLICATE;
        }
        if (result == -1L) {
            return ReserveResult.OUT_OF_STOCK;
        }
        return ReserveResult.SUCCESS;
    }

    public void release(Long productId, Long userId) {
        stringRedisTemplate.execute(
                RELEASE_SCRIPT,
                List.of(buildStockKey(productId), buildUserSetKey(productId)),
                String.valueOf(userId)
        );
    }

    public String buildStockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    public String buildUserSetKey(Long productId) {
        return USER_SET_KEY_PREFIX + productId;
    }

    public enum ReserveResult {
        SUCCESS,
        OUT_OF_STOCK,
        DUPLICATE,
        STOCK_NOT_INITIALIZED,
        ERROR
    }
}

