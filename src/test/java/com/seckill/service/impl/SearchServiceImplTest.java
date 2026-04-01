package com.seckill.service.impl;

import com.seckill.document.SeckillProductDocument;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.repository.SeckillProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private SeckillProductRepository repository;

    @Mock
    private SeckillProductMapper productMapper;

    @InjectMocks
    private SearchServiceImpl searchService;

    @Test
    void searchByKeyword_shouldFallbackToDbWhenElasticsearchIsUnavailable() {
        searchService = new SearchServiceImpl(null, productMapper);
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "iPhone 15 Pro", "苹果 iPhone 15 Pro 256GB 钛金属", "7999"),
                product(2L, "Sony WH-1000XM5", "索尼无线降噪耳机", "1999")
        ));

        Page<SeckillProductDocument> result = searchService.searchByKeyword("iphone", 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("iPhone 15 Pro", result.getContent().get(0).getProductName());
    }

    @Test
    void searchByName_shouldFallbackAndApplyPagination() {
        searchService = new SearchServiceImpl(null, productMapper);
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "iPhone 15", "A", "6999"),
                product(2L, "iPhone 15 Pro", "B", "7999"),
                product(3L, "华为 Mate 60 Pro", "C", "5999")
        ));

        Page<SeckillProductDocument> result = searchService.searchByName("iphone", 1, 1);

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("iPhone 15 Pro", result.getContent().get(0).getProductName());
    }

    @Test
    void searchByPriceRange_shouldFallbackToDbWhenElasticsearchIsUnavailable() {
        searchService = new SearchServiceImpl(null, productMapper);
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "A", "A", "999"),
                product(2L, "B", "B", "1999"),
                product(3L, "C", "C", "5999")
        ));

        Page<SeckillProductDocument> result = searchService.searchByPriceRange(1000.0, 5000.0, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().isEmpty());
        assertEquals("B", result.getContent().get(0).getProductName());
    }

    @Test
    void searchByKeyword_shouldFallbackToDbWhenElasticsearchReturnsEmpty() {
        when(repository.findByProductNameContainingOrDescriptionContaining(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(Page.empty());
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "iPhone 15 Pro", "苹果 iPhone 15 Pro 256GB 钛金属", "7999")
        ));

        Page<SeckillProductDocument> result = searchService.searchByKeyword("苹果", 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("iPhone 15 Pro", result.getContent().get(0).getProductName());
    }

    @Test
    void searchByName_shouldFallbackToDbWhenElasticsearchReturnsEmpty() {
        when(repository.findByProductNameContaining(anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "iPhone 15 Pro", "A", "7999")
        ));

        Page<SeckillProductDocument> result = searchService.searchByName("iPhone", 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("iPhone 15 Pro", result.getContent().get(0).getProductName());
    }

    private SeckillProduct product(Long id, String name, String description, String seckillPrice) {
        SeckillProduct product = new SeckillProduct();
        product.setId(id);
        product.setProductName(name);
        product.setDescription(description);
        product.setOriginalPrice(new BigDecimal(seckillPrice));
        product.setSeckillPrice(new BigDecimal(seckillPrice));
        product.setStock(10);
        product.setStartTime(LocalDateTime.now().minusHours(1));
        product.setEndTime(LocalDateTime.now().plusHours(1));
        return product;
    }
}

