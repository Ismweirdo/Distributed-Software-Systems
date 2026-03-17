package com.seckill.service;

import com.seckill.vo.SeckillProductVO;

public interface CacheService {
    
    /**
     * 从缓存获取商品详情（缓存穿透、击穿、雪崩解决方案）
     * @param productId 商品 ID
     * @return 商品详情
     */
    SeckillProductVO getProductFromCache(Long productId);
    
    /**
     * 将商品详情存入缓存
     * @param productId 商品 ID
     * @param product 商品详情
     */
    void saveProductToCache(Long productId, SeckillProductVO product);
    
    /**
     * 删除缓存
     * @param productId 商品 ID
     */
    void deleteProductCache(Long productId);
    
    /**
     * 使用布隆过滤器判断商品是否存在（防止缓存穿透）
     * @param productId 商品 ID
     * @return true-可能存在，false-一定不存在
     */
    boolean mightExistInBloomFilter(Long productId);
    
    /**
     * 添加商品 ID 到布隆过滤器
     * @param productId 商品 ID
     */
    void addToBloomFilter(Long productId);
}
