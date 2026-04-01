# 秒杀系统（seckill-system）

## 项目简介
本项目是基于 `Spring Boot 3.1` 的秒杀示例系统，包含以下能力：

- 用户注册与登录（`/api/users/*`）
- 商品列表、商品详情、秒杀下单（Redis 预扣 + Kafka 异步创建订单）
- Redis 缓存（空值缓存、互斥锁、随机 TTL）
- 可选的 Elasticsearch 搜索（`/api/search/*`）
- Nginx 负载均衡与动静分离示例配置（`nginx.conf`）

## 当前代码状态
- 默认启动端口是 `8083`（`src/main/resources/application.yml`）。
- `instance1`/`instance2` profile 分别使用 `8081` 和 `8082`。
- Elasticsearch 默认开启：`elasticsearch.enabled=true`（若 ES 不可用，搜索服务会按当前代码逻辑自动降级为可用状态）。
- 读写分离注解与切面已实现（`@DataSourceSlave` + `DataSourceAspect`），并已在 `application.yml` 配置 `master + slave_1 + slave_2` 动态数据源。
- `user` 与 `seckill_product` 已纳入自动初始化，支持首次启动自动建表与商品导入。

## 技术栈
- Java 17
- Spring Boot 3.1.8
- MyBatis-Plus 3.5.6
- dynamic-datasource 4.2.0
- ShardingSphere-JDBC 5.2.1（可选 profile）
- Redis
- MySQL 8
- Elasticsearch（可选）

## 快速开始

### 1. 环境准备
- JDK 17+
- Maven 3.6+
- MySQL 8+
- Redis 6+
- 可选：Elasticsearch 7.x+

### 2. 初始化数据库
先创建数据库（表结构和商品测试数据会在应用启动时自动初始化）：

```sql
CREATE DATABASE IF NOT EXISTS seckill_db DEFAULT CHARSET utf8mb4;
USE seckill_db;
-- 仅需创建数据库本身，后续由应用自动初始化表和基础数据
```

应用启动时会自动执行：
- `src/main/resources/db/init_user.sql`（自动建 `user` 表）
- `src/main/resources/db/init_seckill_product.sql`（自动建 `seckill_product` 表）
- `src/main/resources/db/init_seckill_product_data.sql`（仅在 `seckill_product` 为空时导入测试商品）

### 3. 启动 Redis
示例：

```bash
redis-server
```

### 4. 单实例启动（开发调试）

```bash
mvn clean package -DskipTests
java -jar target/seckill-system-0.0.1-SNAPSHOT.jar
```

服务默认监听：`http://localhost:8083`

### 5. 双实例 + Nginx（负载均衡）

先启动两个后端实例：

```bash
java -jar target/seckill-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=instance1
java -jar target/seckill-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=instance2
```

再配置并启动 Nginx：
- 使用仓库根目录下的 `nginx.conf`
- 按本机路径修改 `root D:/CreatorProject/seckill-system/src/main/resources/static;`
- 访问：`http://localhost/index.html`

### 6. Docker Compose（推荐）
`docker-compose.yml` 可直接拉起 MySQL 主从 + Redis + Elasticsearch + 后端容器，并自动执行主从复制初始化：

```bash
docker-compose up -d
```

端口映射说明：
- MySQL 主库：`3307`
- MySQL 从库 1：`3308`
- MySQL 从库 2：`3309`
- Elasticsearch：`9200`

读写分离说明：
- 主库承担写流量（`master`）
- 从库承担读流量（`slave_1`、`slave_2`，随机路由）
- Compose 启动时会执行 `mysql-replication-init` 一次性任务，完成 `CHANGE REPLICATION SOURCE TO ...` 配置

## 可选：ShardingSphere 分库分表（按 user_id 分库，按 order_id 分表）

该模式与现有 `dynamic-datasource` 读写分离是二选一：
- 默认模式：`seckill.datasource.mode=dynamic`
- 分片模式：`--spring.profiles.active=sharding`（会自动关闭 dynamic-datasource 自动配置）

### 1. 准备两套可写库
- `seckill_db_0`（示例端口 `3310`）
- `seckill_db_1`（示例端口 `3311`）

初始化脚本：
- `src/main/resources/db/sharding/init_sharding_ds0.sql`
- `src/main/resources/db/sharding/init_sharding_ds1.sql`

### 2. 启动分片模式

```bash
java -jar target/seckill-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=sharding
```

可用环境变量：
- `SHARDING_DS0_URL` / `SHARDING_DS0_USERNAME` / `SHARDING_DS0_PASSWORD`
- `SHARDING_DS1_URL` / `SHARDING_DS1_USERNAME` / `SHARDING_DS1_PASSWORD`

### 3. 分片规则
- `t_seckill_order` 逻辑表
- 分库：`ds_${user_id % 2}`
- 分表：`t_seckill_order_${order_id % 2}`

说明：`/api/orders/user?userId=...` 会按用户路由到单库，`/api/orders/{orderId}` 在未携带 `userId` 时可能跨库查询。

## API 说明

统一返回结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

### 用户接口
- `POST /api/users/register`
- `POST /api/users/login`

示例：

```bash
curl -X POST http://localhost:8083/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"123456","phone":"13800000000"}'
```

### 商品接口
- `GET /api/products/list`
- `GET /api/products/{id}`
- `POST /api/products/seckill/{id}?userId={userId}`（返回 `orderId`，表示请求已入队）

示例：

```bash
curl http://localhost:8083/api/products/list
curl http://localhost:8083/api/products/1
curl -X POST "http://localhost:8083/api/products/seckill/1?userId=1001"
```

### 订单接口
- `GET /api/orders/{orderId}`
- `GET /api/orders/user?userId={userId}`

示例：

```bash
curl http://localhost:8083/api/orders/123456789012345678
curl "http://localhost:8083/api/orders/user?userId=1001"
```

### 秒杀链路说明（异步削峰）
- 入口先在 Redis 做原子预扣库存并校验用户是否重复下单。
- 预扣成功后投递 Kafka `seckill.order.create`，由消费者异步创建订单。
- 消费端在数据库事务中执行：条件扣减库存（`stock > 0`）+ 写订单。
- 订单表 `t_seckill_order` 增加唯一键 `uk_user_product(user_id, product_id)`，确保同一用户同一商品只成功一次。

### 搜索接口（可选）
- `GET /api/search/keyword?keyword=手机&page=0&size=10`
- `GET /api/search/name?name=iPhone&page=0&size=10`
- `GET /api/search/price?minPrice=1000&maxPrice=5000&page=0&size=10`
- `POST /api/search/sync`

说明：默认已启用 Elasticsearch。若 ES 暂不可用，搜索服务会按代码中的降级路径执行，不会导致应用启动失败。

### 健康检查接口
- `GET /api/health/init-status`

用于快速确认本次启动是否触发了商品首次导入，返回字段包括：
- `seededThisStartup`：本次启动是否执行了首次导入
- `productCount`：当前商品总数
- `latestSeedAudit`：最近一次导入审计记录（含时间和是否导入）

## Elasticsearch 使用说明

### 1. 配置项
默认配置（`application.yml`）：

```yaml
elasticsearch:
  enabled: ${ELASTICSEARCH_ENABLED:true}

spring:
  data:
    elasticsearch:
      repositories:
        enabled: ${elasticsearch.enabled:false}
      uris: ${SPRING_DATA_ELASTICSEARCH_URIS:http://127.0.0.1:9200}
```

### 2. 启动 ES（可按需安装 IK）

### 3. 触发同步

```bash
curl -X POST http://localhost:8083/api/search/sync
```

## MySQL 读写分离验证

### 1. 准备主从库
项目默认读取如下变量：

- `SPRING_DATASOURCE_MASTER_URL`（默认 `3307`）
- `SPRING_DATASOURCE_SLAVE1_URL`（默认 `3308`）
- `SPRING_DATASOURCE_SLAVE2_URL`（默认 `3309`）

建议直接使用 `docker-compose.yml` 启动主从容器，主机侧映射为 `3307/3308/3309`，且会自动配置复制链路。

### 2. 代码验证
读取操作已标注 `@DataSourceSlave`（例如 `SeckillProductServiceImpl#listProducts/getProductById`），写操作默认走主库。可直接运行：

```bash
mvn test -Dtest=ReadWriteSeparationTest
```

你也可以通过 MySQL 命令手工检查复制状态（容器内）：

```bash
docker exec seckill-mysql-slave1 mysql -uroot -p123456 -e "SHOW REPLICA STATUS\G"
docker exec seckill-mysql-slave2 mysql -uroot -p123456 -e "SHOW REPLICA STATUS\G"
```

关注字段 `Replica_IO_Running: Yes` 与 `Replica_SQL_Running: Yes`。

## 测试

```bash
mvn test -Dtest=CacheServiceTest
mvn test -Dtest=ReadWriteSeparationTest
mvn test -Dtest=SearchServiceTest
```

## 文档索引
- [分布式功能部署指南](docs/分布式功能部署指南.md)
- [分布式功能实现总结](docs/分布式功能实现总结.md)
- [JMeter 测试指南](docs/JMeter%20测试指南.md)
- [优化说明与启动流程](docs/优化说明与启动流程.md)

## 前端页面
静态页面位于 `src/main/resources/static`：
- `/index.html`
- `/main.html`
- `/products.html`

如使用 Nginx，请确保 `root` 指向该目录。
