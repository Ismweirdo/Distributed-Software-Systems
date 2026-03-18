package com.seckill.controller;

import com.seckill.document.SeckillProductDocument;
import com.seckill.service.SearchService;
import com.seckill.vo.Result;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 商品搜索控制器
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 关键词搜索
     * GET /api/search/keyword?keyword=iPhone&page=0&size=10
     */
    @GetMapping("/keyword")
    public Result<Page<SeckillProductDocument>> searchByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<SeckillProductDocument> result = searchService.searchByKeyword(keyword, page, size);
        return Result.success(result);
    }

    /**
     * 按商品名称搜索
     * GET /api/search/name?name=手机&page=0&size=10
     */
    @GetMapping("/name")
    public Result<Page<SeckillProductDocument>> searchByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<SeckillProductDocument> result = searchService.searchByName(name, page, size);
        return Result.success(result);
    }

    /**
     * 价格区间搜索
     * GET /api/search/price?minPrice=1000&maxPrice=5000&page=0&size=10
     */
    @GetMapping("/price")
    public Result<Page<SeckillProductDocument>> searchByPrice(
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<SeckillProductDocument> result = searchService.searchByPriceRange(minPrice, maxPrice, page, size);
        return Result.success(result);
    }

    /**
     * 同步所有商品到 ES
     * POST /api/search/sync
     */
    @PostMapping("/sync")
    public Result<String> syncAllProducts() {
        searchService.syncAllProducts();
        return Result.success("商品数据已同步到 ElasticSearch");
    }
}
