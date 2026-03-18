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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        if (elasticsearchUnavailable("searchByKeyword")) {
            return emptyPage(page, size);
        }
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 关键字搜索: {}, page={}, size={}", keyword, page, size);
        return repository.searchByKeyword(keyword, pageable);
    }

    @Override
    public Page<SeckillProductDocument> searchByName(String name, int page, int size) {
        if (elasticsearchUnavailable("searchByName")) {
            return emptyPage(page, size);
        }
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 商品名称搜索: {}, page={}, size={}", name, page, size);
        return repository.findByProductNameContaining(name, pageable);
    }

    @Override
    public Page<SeckillProductDocument> searchByPriceRange(Double minPrice, Double maxPrice, int page, int size) {
        if (elasticsearchUnavailable("searchByPriceRange")) {
            return emptyPage(page, size);
        }
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 价格区间搜索: {}-{}, page={}, size={}", minPrice, maxPrice, page, size);
        return repository.findByPriceRange(BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice), pageable);
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
        document.setStartTime(product.getStartTime());
        document.setEndTime(product.getEndTime());
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

    private boolean elasticsearchUnavailable(String operation) {
        if (repository != null) {
            return false;
        }
        log.warn("Elasticsearch 不可用，跳过 {} 操作", operation);
        return true;
    }
}
