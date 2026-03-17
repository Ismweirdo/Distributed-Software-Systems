package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.service.CacheService;
import com.seckill.service.SeckillProductService;
import com.seckill.vo.SeckillProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    @Lazy
    private SeckillProductService productService;
    
    @Autowired
    private SeckillProductMapper productMapper;

    // 缓存 Key 前缀
    private static final String PRODUCT_CACHE_KEY = "seckill:product:";
    
    // 布隆过滤器 Key（使用 Bitmap 模拟）
    private static final String BLOOM_FILTER_KEY = "seckill:bloom:product";
    
    // 互斥锁 Key 后缀
    private static final String LOCK_KEY_SUFFIX = ":lock";
    
    // 分布式锁（本地锁，生产环境建议使用 Redisson）
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public SeckillProductVO getProductFromCache(Long productId) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        
        // 1. 先检查布隆过滤器（防止缓存穿透）
        if (!mightExistInBloomFilter(productId)) {
            log.warn("布隆过滤器拦截：商品 ID={} 一定不存在", productId);
            return null;
        }
        
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        
        // 2. 尝试从缓存获取
        Object cached = ops.get(cacheKey);
        if (cached != null) {
            log.info("缓存命中：商品 ID={}", productId);
            return (SeckillProductVO) cached;
        }
        
        // 3. 缓存未命中，尝试获取分布式锁（防止缓存击穿）
        String lockKey = cacheKey + LOCK_KEY_SUFFIX;
        boolean isLocked = lock.tryLock();
        
        try {
            if (isLocked) {
                // 双重检查缓存（可能其他线程已经写入）
                cached = ops.get(cacheKey);
                if (cached != null) {
                    log.info("双重检查缓存命中：商品 ID={}", productId);
                    return (SeckillProductVO) cached;
                }
                
                // 4. 从数据库查询
                log.info("缓存未命中，查询数据库：商品 ID={}", productId);
                SeckillProduct product = productService.getById(productId);
                
                if (product == null) {
                    // 数据库也没有，设置空值缓存（防止缓存穿透）
                    // 使用较短过期时间（30 秒）
                    redisTemplate.opsForValue().set(cacheKey, null, 30, TimeUnit.SECONDS);
                    log.warn("数据库无此商品，设置空值缓存：商品 ID={}", productId);
                    return null;
                }
                
                // 5. 转换为 VO
                SeckillProductVO vo = new SeckillProductVO();
                BeanUtils.copyProperties(product, vo);
                
                // 6. 写入缓存（使用随机过期时间，防止缓存雪崩）
                // 基础过期时间 30 分钟 + 随机 0-10 分钟
                long expireTime = 30 + (long)(Math.random() * 10);
                saveProductToCache(productId, vo);
                log.info("缓存写入成功：商品 ID={}, 过期时间={}分钟", productId, expireTime);
                
                // 7. 添加到布隆过滤器
                addToBloomFilter(productId);
                
                return vo;
            } else {
                // 未获取到锁，等待 100ms 后重试（或直接返回缓存）
                log.info("未获取到锁，等待后重试：商品 ID={}", productId);
                Thread.sleep(100);
                return getProductFromCache(productId); // 递归调用
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取缓存时中断：商品 ID={}", productId, e);
            return null;
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }

    @Override
    public void saveProductToCache(Long productId, SeckillProductVO product) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        // 基础过期时间 30 分钟 + 随机 0-10 分钟（防止雪崩）
        long expireTime = 30 + (long)(Math.random() * 10);
        redisTemplate.opsForValue().set(cacheKey, product, expireTime, TimeUnit.MINUTES);
    }

    @Override
    public void deleteProductCache(Long productId) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        redisTemplate.delete(cacheKey);
        log.info("缓存删除成功：商品 ID={}", productId);
    }

    @Override
    public boolean mightExistInBloomFilter(Long productId) {
        // 使用 Redis Bitmap 模拟布隆过滤器
        // 简单实现：直接检查对应位是否为 1
        String key = BLOOM_FILTER_KEY;
        Boolean bit = redisTemplate.opsForValue().getBit(key, productId % 1000000);
        return bit != null && bit;
    }

    @Override
    public void addToBloomFilter(Long productId) {
        // 使用 Redis Bitmap 模拟布隆过滤器
        String key = BLOOM_FILTER_KEY;
        redisTemplate.opsForValue().setBit(key, productId % 1000000, true);
    }
}
