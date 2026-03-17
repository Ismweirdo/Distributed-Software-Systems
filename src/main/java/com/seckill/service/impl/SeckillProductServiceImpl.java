package com.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.service.CacheService;
import com.seckill.service.SeckillProductService;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> 
        implements SeckillProductService {

    private final CacheService cacheService;

    public SeckillProductServiceImpl(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public Result<List<SeckillProductVO>> listProducts() {
        List<SeckillProduct> products = list();
        List<SeckillProductVO> productVOList = products.stream()
                .map(product -> {
                    SeckillProductVO vo = new SeckillProductVO();
                    BeanUtils.copyProperties(product, vo);
                    return vo;
                })
                .collect(Collectors.toList());
        return Result.success(productVOList);
    }

    @Override
    public Result<SeckillProductVO> getProductById(Long id) {
        // 使用缓存获取商品详情（包含穿透/击穿/雪崩解决方案）
        SeckillProductVO product = cacheService.getProductFromCache(id);
        if (product == null) {
            return Result.fail("商品不存在或已被删除");
        }
        return Result.success(product);
    }

    @Override
    public Result<Boolean> seckillProduct(Long productId) {
        // 简单的库存扣减逻辑
        SeckillProduct product = getById(productId);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        if (product.getStock() <= 0) {
            return Result.fail("库存不足");
        }
        
        // 扣减库存
        product.setStock(product.getStock() - 1);
        product.setUpdateTime(LocalDateTime.now());
        updateById(product);
        
        // 删除缓存（下次查询时会重新加载）
        cacheService.deleteProductCache(productId);
        
        return Result.success(true);
    }
}
