package com.seckill.controller;

import com.seckill.document.SeckillProductDocument;
import com.seckill.service.SearchService;
import com.seckill.vo.Result;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Validated
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/keyword")
    public Result<Page<SeckillProductDocument>> searchByKeyword(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(searchService.searchByKeyword(keyword.trim(), page, size));
    }

    @GetMapping("/name")
    public Result<Page<SeckillProductDocument>> searchByName(
            @RequestParam @NotBlank String name,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(searchService.searchByName(name.trim(), page, size));
    }

    @GetMapping("/price")
    public Result<Page<SeckillProductDocument>> searchByPrice(
            @RequestParam @NotNull @DecimalMin(value = "0.0", message = "最小价格不能小于 0") Double minPrice,
            @RequestParam @NotNull @DecimalMin(value = "0.0", message = "最大价格不能小于 0") Double maxPrice,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (minPrice > maxPrice) {
            log.info("收到反向价格区间，自动交换 minPrice={} maxPrice={}", minPrice, maxPrice);
            double tmp = minPrice;
            minPrice = maxPrice;
            maxPrice = tmp;
        }
        return Result.success(searchService.searchByPriceRange(minPrice, maxPrice, page, size));
    }

    @PostMapping("/sync")
    public Result<String> syncAllProducts() {
        searchService.syncAllProducts();
        return Result.success("已触发同步流程（若 Elasticsearch 未启用，将自动跳过）");
    }
}
