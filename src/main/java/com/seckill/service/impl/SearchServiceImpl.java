package com.seckill.service.impl;

import com.seckill.document.SeckillProductDocument;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.repository.SeckillProductRepository;
import com.seckill.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final SeckillProductRepository repository;
    private final SeckillProductMapper productMapper;

    public SearchServiceImpl(SeckillProductRepository repository, SeckillProductMapper productMapper) {
        this.repository = repository;
        this.productMapper = productMapper;
    }

    @Override
    public SeckillProductDocument save(SeckillProductDocument product) {
        log.info("保存商品到 ES: {}", product.getProductName());
        return repository.save(product);
    }

    @Override
    public List<SeckillProductDocument> saveAll(List<SeckillProductDocument> products) {
        log.info("批量保存 {} 个商品到 ES", products.size());
        return (List<SeckillProductDocument>) repository.saveAll(products);
    }

    @Override
    public void delete(Long id) {
        log.info("从 ES 删除商品：{}", id);
        repository.deleteById(id);
    }

    @Override
    public SeckillProductDocument getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Page<SeckillProductDocument> searchByKeyword(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 关键词搜索：{}, 页码：{}, 大小：{}", keyword, page, size);
        return repository.searchByKeyword(keyword, pageable);
    }

    @Override
    public Page<SeckillProductDocument> searchByName(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 按名称搜索：{}, 页码：{}, 大小：{}", name, page, size);
        return repository.findByProductNameContaining(name, pageable);
    }

    @Override
    public Page<SeckillProductDocument> searchByPriceRange(Double minPrice, Double maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        log.info("ES 价格区间搜索：{} - {}, 页码：{}, 大小：{}", minPrice, maxPrice, page, size);
        return repository.findByPriceRange(java.math.BigDecimal.valueOf(minPrice), 
                                          java.math.BigDecimal.valueOf(maxPrice), pageable);
    }

    @Override
    public void syncAllProducts() {
        log.info("开始同步所有商品到 ES");
        
        // 从数据库查询所有商品
        List<SeckillProduct> products = productMapper.selectList(null);
        
        // 转换为 ES 文档
        List<SeckillProductDocument> documents = products.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
        
        // 批量保存到 ES
        saveAll(documents);
        
        log.info("成功同步 {} 个商品到 ES", documents.size());
    }

    /**
     * 将实体转换为 ES 文档
     */
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
        
        // 设置秒杀状态
        LocalDateTime now = LocalDateTime.now();
        if (product.getStartTime().isAfter(now)) {
            document.setStatus("UPCOMING");
        } else if (product.getEndTime().isBefore(now)) {
            document.setStatus("ENDED");
        } else {
            document.setStatus("ONGOING");
        }
        
        return document;
    }
}
