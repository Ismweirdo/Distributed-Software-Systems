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

-- 插入测试数据
INSERT INTO `seckill_product` (`product_name`, `description`, `original_price`, `seckill_price`, `stock`, `start_time`, `end_time`) VALUES
('iPhone 15 Pro', '苹果 iPhone 15 Pro 256GB 钛金属', 8999.00, 7999.00, 100, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('华为 Mate 60 Pro', '华为 Mate 60 Pro 512GB 雅川青', 6999.00, 5999.00, 150, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('小米 14 Ultra', '小米 14 Ultra 16GB+512GB 钛金属', 6499.00, 5499.00, 200, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('MacBook Pro 14', '苹果 MacBook Pro 14 寸 M3 芯片 16GB 512GB', 12999.00, 10999.00, 50, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('Sony WH-1000XM5', '索尼 WH-1000XM5 无线降噪耳机', 2499.00, 1999.00, 300, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('iPad Air 5', '苹果 iPad Air 5 10.9 英寸 256GB WiFi 版', 4799.00, 3999.00, 120, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('Switch OLED', '任天堂 Switch OLED 版 日版', 2099.00, 1799.00, 80, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('PS5', '索尼 PlayStation 5 光驱版', 3899.00, 3499.00, 60, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY));

-- 查看插入的数据
SELECT * FROM seckill_product;
