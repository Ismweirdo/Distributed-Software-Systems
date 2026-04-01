package com.seckill.service.impl;

import com.seckill.document.SeckillProductDocument;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.repository.SeckillProductRepository;
import com.seckill.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final SeckillProductRepository repository;
    private final SeckillProductMapper productMapper;

    public SearchServiceImpl(@Autowired(required = false) SeckillProductRepository repository,
                             SeckillProductMapper productMapper) {
        this.repository = repository;
        this.productMapper = productMapper;
    }

    @Override
    public SeckillProductDocument save(SeckillProductDocument product) {
        if (elasticsearchUnavailable("save")) {
            return product;
        }
        log.info("保存商品到 ES: {}", product.getProductName());
        return repository.save(product);
    }

    @Override
    public List<SeckillProductDocument> saveAll(List<SeckillProductDocument> products) {
        if (elasticsearchUnavailable("saveAll")) {
            return products;
        }
        log.info("批量保存 {} 个商品到 ES", products.size());
        List<SeckillProductDocument> savedDocuments = new ArrayList<>();
        repository.saveAll(products).forEach(savedDocuments::add);
        return savedDocuments;
    }

    @Override
    public void delete(Long id) {
        if (elasticsearchUnavailable("delete")) {
            return;
        }
        log.info("从 ES 删除商品: {}", id);
        repository.deleteById(id);
    }

    @Override
    public SeckillProductDocument getById(Long id) {
        if (elasticsearchUnavailable("getById")) {
            return null;
        }
        return repository.findById(id).orElse(null);
    }

    @Override
    public Page<SeckillProductDocument> searchByKeyword(String keyword, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (elasticsearchUnavailable("searchByKeyword")) {
            return searchKeywordFromDb(normalizedKeyword, page, size);
        }
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 关键字搜索: {}, page={}, size={}", normalizedKeyword, page, size);
        try {
            Page<SeckillProductDocument> esResult = repository.findByProductNameContainingOrDescriptionContaining(normalizedKeyword, normalizedKeyword, pageable);
            if (esResult.hasContent()) {
                return esResult;
            }
            log.info("ES 关键字搜索结果为空，回退 DB 搜索: keyword={}, page={}, size={}", normalizedKeyword, page, size);
            return searchKeywordFromDb(normalizedKeyword, page, size);
        } catch (Exception e) {
            log.warn("ES 查询失败，使用内存回退：{}", e.getMessage());
            return searchKeywordFromDb(normalizedKeyword, page, size);
        }
    }

    @Override
    public Page<SeckillProductDocument> searchByName(String name, int page, int size) {
        String normalizedName = name == null ? "" : name.trim();
        if (elasticsearchUnavailable("searchByName")) {
            return searchNameFromDb(normalizedName, page, size);
        }
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 商品名称搜索: {}, page={}, size={}", normalizedName, page, size);
        try {
            Page<SeckillProductDocument> esResult = repository.findByProductNameContaining(normalizedName, pageable);
            if (esResult.hasContent()) {
                return esResult;
            }
            log.info("ES 名称搜索结果为空，回退 DB 搜索: name={}, page={}, size={}", normalizedName, page, size);
            return searchNameFromDb(normalizedName, page, size);
        } catch (Exception e) {
            log.warn("ES 名称查询失败，使用内存回退：{}", e.getMessage());
            return searchNameFromDb(normalizedName, page, size);
        }
    }

    @Override
    public Page<SeckillProductDocument> searchByPriceRange(Double minPrice, Double maxPrice, int page, int size) {
        if (elasticsearchUnavailable("searchByPriceRange")) {
            return searchPriceRangeFromDb(minPrice, maxPrice, page, size);
        }
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 价格区间搜索: {}-{}, page={}, size={}", minPrice, maxPrice, page, size);
        try {
            Page<SeckillProductDocument> esResult = repository.findBySeckillPriceBetween(BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice), pageable);
            if (esResult.hasContent()) {
                return esResult;
            }
            log.info("ES 价格区间搜索结果为空，回退 DB 搜索: min={}, max={}, page={}, size={}", minPrice, maxPrice, page, size);
            return searchPriceRangeFromDb(minPrice, maxPrice, page, size);
        } catch (Exception e) {
            log.warn("ES 价格区间查询失败，使用内存回退：{}", e.getMessage());
            return searchPriceRangeFromDb(minPrice, maxPrice, page, size);
        }
    }

    @Override
    public void syncAllProducts() {
        if (elasticsearchUnavailable("syncAllProducts")) {
            return;
        }
        log.info("开始同步所有商品到 ES");

        List<SeckillProduct> products = productMapper.selectList(null);
        if (products.isEmpty()) {
            log.info("数据库中没有商品，跳过 ES 同步");
            return;
        }

        List<SeckillProductDocument> documents = products.stream()
                .map(this::convertToDocument)
                .toList();

        saveAll(documents);
        log.info("成功同步 {} 个商品到 ES", documents.size());
    }

    private SeckillProductDocument convertToDocument(SeckillProduct product) {
        SeckillProductDocument document = new SeckillProductDocument();
        document.setId(product.getId());
        document.setProductName(product.getProductName());
        document.setDescription(product.getDescription());
        document.setOriginalPrice(product.getOriginalPrice());
        document.setSeckillPrice(product.getSeckillPrice());
        document.setStock(product.getStock());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        document.setStartTime(product.getStartTime() == null ? null : product.getStartTime().format(fmt));
        document.setEndTime(product.getEndTime() == null ? null : product.getEndTime().format(fmt));
        document.setStatus(resolveStatus(product.getStartTime(), product.getEndTime()));
        return document;
    }

    private String resolveStatus(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "UNKNOWN";
        }

        LocalDateTime now = LocalDateTime.now();
        if (startTime.isAfter(now)) {
            return "UPCOMING";
        }
        if (endTime.isBefore(now)) {
            return "ENDED";
        }
        return "ONGOING";
    }

    private Page<SeckillProductDocument> emptyPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    private Page<SeckillProductDocument> searchKeywordFromDb(String keyword, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<SeckillProductDocument> docs = productMapper.selectList(null)
                .stream()
                .map(this::convertToDocument)
                .filter(d -> containsIgnoreCase(d.getProductName(), normalizedKeyword)
                        || containsIgnoreCase(d.getDescription(), normalizedKeyword))
                .toList();
        return toPage(docs, page, size);
    }

    private Page<SeckillProductDocument> searchNameFromDb(String name, int page, int size) {
        String normalizedName = name == null ? "" : name.trim();
        List<SeckillProductDocument> docs = productMapper.selectList(null)
                .stream()
                .map(this::convertToDocument)
                .filter(d -> containsIgnoreCase(d.getProductName(), normalizedName))
                .toList();
        return toPage(docs, page, size);
    }

    private Page<SeckillProductDocument> searchPriceRangeFromDb(Double minPrice, Double maxPrice, int page, int size) {
        BigDecimal min = BigDecimal.valueOf(minPrice);
        BigDecimal max = BigDecimal.valueOf(maxPrice);
        List<SeckillProductDocument> docs = productMapper.selectList(null)
                .stream()
                .map(this::convertToDocument)
                .filter(d -> {
                    BigDecimal price = d.getSeckillPrice();
                    return price != null && price.compareTo(min) >= 0 && price.compareTo(max) <= 0;
                })
                .toList();
        return toPage(docs, page, size);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        if (source == null || source.isBlank()) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private Page<SeckillProductDocument> toPage(List<SeckillProductDocument> docs, int page, int size) {
        if (docs.isEmpty()) {
            return emptyPage(page, size);
        }
        Pageable pageable = PageRequest.of(page, size);
        int start = Math.min((int) pageable.getOffset(), docs.size());
        int end = Math.min(start + pageable.getPageSize(), docs.size());
        return new PageImpl<>(docs.subList(start, end), pageable, docs.size());
    }

    private boolean elasticsearchUnavailable(String operation) {
        if (repository != null) {
            return false;
        }
        log.warn("Elasticsearch 不可用，跳过 {} 操作", operation);
        return true;
    }
}
