-- 创建秒杀商品表
CREATE TABLE IF NOT EXISTS `seckill_product` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '商品 ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `description` TEXT COMMENT '商品描述',
    `original_price` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '原价',
    `seckill_price` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '秒杀价',
    `stock` INT(11) NOT NULL DEFAULT '0' COMMENT '库存数量',
    `start_time` DATETIME DEFAULT NULL COMMENT '秒杀开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '秒杀结束时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';
