CREATE TABLE IF NOT EXISTS `startup_init_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `event_type` VARCHAR(64) NOT NULL,
  `seeded` TINYINT(1) NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_event_type_create_time` (`event_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @seed_inserted := 0;

INSERT INTO `seckill_product` (`product_name`, `description`, `original_price`, `seckill_price`, `stock`, `start_time`, `end_time`)
SELECT t.product_name, t.description, t.original_price, t.seckill_price, t.stock, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)
FROM (
    SELECT 'iPhone 15 Pro' AS product_name, '苹果 iPhone 15 Pro 256GB 钛金属' AS description, 8999.00 AS original_price, 7999.00 AS seckill_price, 100 AS stock
    UNION ALL
    SELECT '华为 Mate 60 Pro', '华为 Mate 60 Pro 512GB 雅川青', 6999.00, 5999.00, 150
    UNION ALL
    SELECT '小米 14 Ultra', '小米 14 Ultra 16GB+512GB 钛金属', 6499.00, 5499.00, 200
    UNION ALL
    SELECT 'MacBook Pro 14', '苹果 MacBook Pro 14 寸 M3 芯片 16GB 512GB', 12999.00, 10999.00, 50
    UNION ALL
    SELECT 'Sony WH-1000XM5', '索尼 WH-1000XM5 无线降噪耳机', 2499.00, 1999.00, 300
    UNION ALL
    SELECT 'iPad Air 5', '苹果 iPad Air 5 10.9 英寸 256GB WiFi 版', 4799.00, 3999.00, 120
    UNION ALL
    SELECT 'Switch OLED', '任天堂 Switch OLED 版 日版', 2099.00, 1799.00, 80
    UNION ALL
    SELECT 'PS5', '索尼 PlayStation 5 光驱版', 3899.00, 3499.00, 60
) t
WHERE NOT EXISTS (SELECT 1 FROM `seckill_product` LIMIT 1);

SET @seed_inserted := ROW_COUNT();

INSERT INTO `startup_init_audit` (`event_type`, `seeded`)
VALUES ('seckill_product_seed', IF(@seed_inserted > 0, 1, 0));
