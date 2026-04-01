package com.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seckill.entity.SeckillProduct;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;

import java.util.List;

public interface SeckillProductService extends IService<SeckillProduct> {
    Result<List<SeckillProductVO>> listProducts();
    Result<SeckillProductVO> getProductById(Long id);
    Result<Boolean> seckillProduct(Long productId);
    Result<Long> seckillProduct(Long productId, Long userId);
}
