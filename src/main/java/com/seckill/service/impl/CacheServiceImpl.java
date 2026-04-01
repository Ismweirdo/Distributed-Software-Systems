package com.seckill.service.impl;

import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.service.CacheService;
import com.seckill.vo.SeckillProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "seckill:product:";
    private static final String BLOOM_FILTER_KEY = "seckill:bloom:product";
    private static final String NULL_CACHE_VALUE = "__NULL__";

    private static final long BLOOM_FILTER_BUCKET_SIZE = 1_000_000L;
    private static final int BASE_EXPIRE_MINUTES = 30;
    private static final int RANDOM_EXPIRE_MINUTES = 10;
    private static final int NULL_CACHE_SECONDS = 30;

    private static final int RETRY_TIMES = 3;
    private static final long RETRY_INTERVAL_MILLIS = 80L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SeckillProductMapper productMapper;

    // 使用 Redis 分布式锁，避免单机 ReentrantLock 无法跨实例生效

    @Override
    public SeckillProductVO getProductFromCache(Long productId) {
        if (productId == null || productId <= 0) {
            return null;
        }

        String cacheKey = buildProductCacheKey(productId);
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return unwrapCacheValue(cachedValue, productId);
        }

        // 布隆过滤器判断（如果确定不存在，避免穿透到 DB）
        if (!mightExistInBloomFilter(productId)) {
            log.info("布隆过滤器判定不存在: productId={}", productId);
            return null;
        }

        String lockKey = cacheKey + ":lock";
        String lockVal = UUID.randomUUID().toString();
        boolean locked = tryAcquireLock(lockKey, lockVal, 5000);
        if (!locked) {
            return readCacheAfterRetry(cacheKey, productId);
        }

        try {
            Object latestCachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (latestCachedValue != null) {
                return unwrapCacheValue(latestCachedValue, productId);
            }
            SeckillProduct product = productMapper.selectById(productId);
            if (product == null) {
                cacheNullValue(cacheKey);
                log.info("缓存空值写入成功: productId={}", productId);
                return null;
            }

            SeckillProductVO productVO = convertToVO(product);
            saveProductToCache(productId, productVO);
            addToBloomFilter(productId);
            return productVO;
        } finally {
            if (locked) {
                releaseLock(lockKey, lockVal);
            }
        }
    }

    @Override
    public void saveProductToCache(Long productId, SeckillProductVO product) {
        String cacheKey = buildProductCacheKey(productId);
        long expireMinutes = BASE_EXPIRE_MINUTES + ThreadLocalRandom.current().nextLong(RANDOM_EXPIRE_MINUTES + 1L);
        redisTemplate.opsForValue().set(cacheKey, product, expireMinutes, TimeUnit.MINUTES);
        log.info("缓存写入成功: productId={}, expireMinutes={}", productId, expireMinutes);
    }

    @Override
    public void deleteProductCache(Long productId) {
        redisTemplate.delete(buildProductCacheKey(productId));
        log.info("缓存删除成功: productId={}", productId);
    }

    @Override
    public boolean mightExistInBloomFilter(Long productId) {
        if (productId == null || productId <= 0) {
            return false;
        }
        Boolean bit = redisTemplate.opsForValue().getBit(BLOOM_FILTER_KEY, bloomOffset(productId));
        return Boolean.TRUE.equals(bit);
    }

    @Override
    public void addToBloomFilter(Long productId) {
        if (productId == null || productId <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().setBit(BLOOM_FILTER_KEY, bloomOffset(productId), true);
        } catch (Exception e) {
            // If Redis is unavailable during startup or tests, log and continue — Bloom filter is an optimization.
            log.warn("无法连接到Redis，正在跳过将商品加入布隆过滤器: productId={},原因={}", productId, e.getMessage());
        }
    }

    private SeckillProductVO readCacheAfterRetry(String cacheKey, Long productId) {
        for (int i = 0; i < RETRY_TIMES; i++) {
            try {
                Thread.sleep(RETRY_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("缓存重试被中断: productId={}", productId, e);
                return null;
            }

            Object retriedValue = redisTemplate.opsForValue().get(cacheKey);
            if (retriedValue != null) {
                return unwrapCacheValue(retriedValue, productId);
            }
        }

        log.warn("缓存重试结束仍未命中: productId={}", productId);
        return null;
    }

    private SeckillProductVO unwrapCacheValue(Object cacheValue, Long productId) {
        if (NULL_CACHE_VALUE.equals(cacheValue)) {
            return null;
        }
        if (cacheValue instanceof SeckillProductVO productVO) {
            log.info("缓存命中: productId={}", productId);
            return productVO;
        }

        log.warn("缓存中存在未知类型数据: productId={}, type={}", productId, cacheValue.getClass().getName());
        return null;
    }

    private void cacheNullValue(String cacheKey) {
        redisTemplate.opsForValue().set(cacheKey, NULL_CACHE_VALUE, NULL_CACHE_SECONDS, TimeUnit.SECONDS);
    }

    private SeckillProductVO convertToVO(SeckillProduct product) {
        SeckillProductVO vo = new SeckillProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }

    private String buildProductCacheKey(Long productId) {
        return PRODUCT_CACHE_KEY_PREFIX + productId;
    }

    private long bloomOffset(Long productId) {
        return Math.floorMod(productId, BLOOM_FILTER_BUCKET_SIZE);
    }

    /* ---------- Redis 分布式锁相关方法 ---------- */
    private boolean tryAcquireLock(String lockKey, String lockVal, long expireMillis) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockVal, expireMillis, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(String lockKey, String lockVal) {
        String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(lua, Long.class);
        try {
            redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockVal);
        } catch (Exception e) {
            log.warn("释放分布式锁失败: {}", lockKey, e);
        }
    }
}

