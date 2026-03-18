package com.seckill;

import com.seckill.service.CacheService;
import com.seckill.service.SeckillProductService;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Redis 缓存功能测试
 */
@SpringBootTest
public class CacheServiceTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private SeckillProductService productService;

    /**
     * 测试缓存穿透 - 查询不存在的数据
     */
    @Test
    public void testCachePenetration() {
        System.out.println("=== 测试缓存穿透 ===");
        
        // 第一次查询不存在的 ID
        Result<SeckillProductVO> result1 = productService.getProductById(999999L);
        System.out.println("第一次查询：" + result1);
        
        // 第二次查询同样的 ID（应该被空值缓存拦截）
        Result<SeckillProductVO> result2 = productService.getProductById(999999L);
        System.out.println("第二次查询：" + result2);
    }

    /**
     * 测试缓存击穿 - 多个线程同时查询同一个热点数据
     */
    @Test
    public void testCacheBreakdown() throws InterruptedException {
        System.out.println("\n=== 测试缓存击穿 ===");
        
        // 假设商品 ID=1 是热点数据
        Long productId = 1L;
        
        // 模拟 10 个线程同时查询
        for (int i = 0; i < 10; i++) {
            final int threadNum = i;
            new Thread(() -> {
                Result<SeckillProductVO> result = productService.getProductById(productId);
                System.out.println("线程 " + threadNum + " 查询结果：" + 
                    (result.getData() != null ? result.getData().getProductName() : "null"));
            }).start();
        }
        
        Thread.sleep(2000); // 等待所有线程执行完毕
    }

    /**
     * 测试缓存雪崩 - 设置不同的过期时间
     */
    @Test
    public void testCacheAvalanche() {
        System.out.println("\n=== 测试缓存雪崩 ===");
        
        // 查询多个商品，它们的过期时间会不同
        for (long i = 1; i <= 5; i++) {
            Result<SeckillProductVO> result = productService.getProductById(i);
            if (result.getData() != null) {
                System.out.println("商品 " + i + ": " + result.getData().getProductName());
            }
        }
    }

    /**
     * 测试布隆过滤器
     */
    @Test
    public void testBloomFilter() {
        System.out.println("\n=== 测试布隆过滤器 ===");
        
        // 先查询一些存在的商品
        for (long i = 1; i <= 3; i++) {
            Result<SeckillProductVO> result = productService.getProductById(i);
            if (result.getData() != null) {
                System.out.println("查询商品 " + i + ": " + result.getData().getProductName());
            }
        }
        
        // 然后查询不存在的商品（应该被布隆过滤器拦截）
        System.out.println("\n查询不存在的商品：");
        Result<SeckillProductVO> result = productService.getProductById(888888L);
        System.out.println("查询结果：" + result);
    }
}
