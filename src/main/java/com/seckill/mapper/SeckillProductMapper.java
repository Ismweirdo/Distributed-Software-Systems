package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeckillProductMapper extends BaseMapper<SeckillProduct> {

    /**
     * 仅查询所有商品 ID，用于初始化布隆过滤器
     */
    default java.util.List<Long> selectAllIds() {
        return this.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SeckillProduct>().select("id"))
                .stream()
                .map(SeckillProduct::getId)
                .toList();
    }
}
