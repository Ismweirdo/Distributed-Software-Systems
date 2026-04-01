package com.seckill;

import com.seckill.document.SeckillProductDocument;
import com.seckill.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * ElasticSearch 搜索功能测试
 */
@SpringBootTest
public class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    /**
     * 测试同步所有商品到 ES
     */
    @Test
    public void testSyncAllProducts() {
        searchService.syncAllProducts();
    }

    /**
     * 测试关键词搜索
     */
    @Test
    public void testSearchByKeyword() {
        // 先同步数据
        searchService.syncAllProducts();

        // 测试搜索
        Page<SeckillProductDocument> result = searchService.searchByKeyword("iPhone", 0, 10);
        assertNotNull(result);
        assertFalse(result.getContent().isEmpty());
        assertTrue(result.getContent().stream().anyMatch(product ->
                product.getProductName() != null && product.getProductName().contains("iPhone")
        ));
        System.out.println("搜索 'iPhone' 的结果数：" + result.getTotalElements());
        result.forEach(product ->
            System.out.println("商品：" + product.getProductName() + ", 价格：" + product.getSeckillPrice())
        );
    }

    /**
     * 测试中文关键词搜索（不同分词策略下结果数可能不同，但调用应稳定）
     */
    @Test
    public void testSearchByChineseKeyword() {
        searchService.syncAllProducts();
        Page<SeckillProductDocument> result = searchService.searchByKeyword("苹果", 0, 10);
        assertNotNull(result);
        assertNotNull(result.getContent());
    }

    /**
     * 测试按名称搜索
     */
    @Test
    public void testSearchByName() {
        searchService.syncAllProducts();
        Page<SeckillProductDocument> result = searchService.searchByName("iPhone", 0, 10);
        assertNotNull(result);
        assertTrue(result.getTotalElements() > 0);
        assertFalse(result.getContent().isEmpty());
        System.out.println("搜索 'iPhone' 的结果数：" + result.getTotalElements());
        result.forEach(product -> 
            System.out.println("商品：" + product.getProductName() + ", 库存：" + product.getStock())
        );
    }

    /**
     * 测试价格区间搜索
     */
    @Test
    public void testSearchByPriceRange() {
        searchService.syncAllProducts();
        Page<SeckillProductDocument> result = searchService.searchByPriceRange(1000.0, 5000.0, 0, 10);
        assertNotNull(result);
        assertTrue(result.getTotalElements() > 0);
        assertFalse(result.getContent().isEmpty());
        System.out.println("价格区间 [1000-5000] 的结果数：" + result.getTotalElements());
        result.forEach(product -> 
            System.out.println("商品：" + product.getProductName() + ", 价格：" + product.getSeckillPrice())
        );
    }
}
