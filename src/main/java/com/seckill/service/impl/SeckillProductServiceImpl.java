package com.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.annotation.DataSourceSlave;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.service.CacheService;
import com.seckill.service.SeckillProductService;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct>
        implements SeckillProductService {

    private final CacheService cacheService;

    @Override
    @DataSourceSlave
    public Result<List<SeckillProductVO>> listProducts() {
        List<SeckillProductVO> productVOList = list().stream()
                .map(this::convertToVO)
                .toList();
        return Result.success(productVOList);
    }

    @Override
    @DataSourceSlave
    public Result<SeckillProductVO> getProductById(Long id) {
        SeckillProductVO product = cacheService.getProductFromCache(id);
        if (product == null) {
            return Result.fail("商品不存在或已被删除");
        }
        return Result.success(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> seckillProduct(Long productId) {
        boolean updated = lambdaUpdate()
                .setSql("stock = stock - 1")
                .set(SeckillProduct::getUpdateTime, LocalDateTime.now())
                .eq(SeckillProduct::getId, productId)
                .gt(SeckillProduct::getStock, 0)
                .update();

        if (!updated) {
            SeckillProduct product = getById(productId);
            if (product == null) {
                return Result.fail("商品不存在");
            }
            return Result.fail("库存不足");
        }

        cacheService.deleteProductCache(productId);
        return Result.success(true);
    }

    private SeckillProductVO convertToVO(SeckillProduct product) {
        SeckillProductVO vo = new SeckillProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
}
