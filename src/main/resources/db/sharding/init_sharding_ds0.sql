CREATE DATABASE IF NOT EXISTS `seckill_db_0` DEFAULT CHARSET utf8mb4;
USE `seckill_db_0`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS `t_seckill_order_0` (
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `order_amount` DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
  `order_status` VARCHAR(32) NOT NULL COMMENT '订单状态',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单分表0';

CREATE TABLE IF NOT EXISTS `t_seckill_order_1` (
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `order_amount` DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
  `order_status` VARCHAR(32) NOT NULL COMMENT '订单状态',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单分表1';

INSERT INTO `seckill_product` (`id`, `product_name`, `description`, `original_price`, `seckill_price`, `stock`, `start_time`, `end_time`)
SELECT `id`, `product_name`, `description`, `original_price`, `seckill_price`, `stock`, `start_time`, `end_time`
FROM (
  SELECT 1 AS `id`, 'iPhone 15 Pro' AS `product_name`, 'Apple 手机' AS `description`, 8999.00 AS `original_price`, 6999.00 AS `seckill_price`, 50 AS `stock`, NOW() AS `start_time`, DATE_ADD(NOW(), INTERVAL 1 DAY) AS `end_time`
  UNION ALL
  SELECT 2, '华为 Mate 60', '华为旗舰手机', 6999.00, 5499.00, 80, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY)
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM `seckill_product` LIMIT 1);

