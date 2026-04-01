package com.seckill.repository;

import com.seckill.document.SeckillProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * 商品 ES 仓库接口
 */
@Repository
public interface SeckillProductRepository extends ElasticsearchRepository<SeckillProductDocument, Long> {

    /**
     * 根据商品名称搜索
     */
    Page<SeckillProductDocument> findByProductNameContaining(String productName, Pageable pageable);

    /**
     * 根据描述搜索
     */
    Page<SeckillProductDocument> findByDescriptionContaining(String description, Pageable pageable);

    /**
     * 多字段匹配搜索（衍生查询）
     */
    Page<SeckillProductDocument> findByProductNameContainingOrDescriptionContaining(String name, String description, Pageable pageable);

    /**
     * 查询指定价格区间的商品（衍生查询）
     */
    Page<SeckillProductDocument> findBySeckillPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 查询有库存的商品（衍生查询）
     */
    Page<SeckillProductDocument> findByStockGreaterThan(Integer stock, Pageable pageable);
}
