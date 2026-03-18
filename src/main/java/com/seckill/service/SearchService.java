package com.seckill.service;

import com.seckill.document.SeckillProductDocument;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * ElasticSearch 搜索服务
 */
public interface SearchService {

    /**
     * 保存商品到 ES
     */
    SeckillProductDocument save(SeckillProductDocument product);

    /**
     * 批量保存商品
     */
    List<SeckillProductDocument> saveAll(List<SeckillProductDocument> products);

    /**
     * 删除商品
     */
    void delete(Long id);

    /**
     * 根据 ID 查询
     */
    SeckillProductDocument getById(Long id);

    /**
     * 关键词搜索（商品名称 + 描述）
     */
    Page<SeckillProductDocument> searchByKeyword(String keyword, int page, int size);

    /**
     * 根据商品名称搜索
     */
    Page<SeckillProductDocument> searchByName(String name, int page, int size);

    /**
     * 价格区间搜索
     */
    Page<SeckillProductDocument> searchByPriceRange(Double minPrice, Double maxPrice, int page, int size);

    /**
     * 同步所有商品到 ES
     */
    void syncAllProducts();
}
