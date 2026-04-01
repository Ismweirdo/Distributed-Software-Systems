# 秒杀系统（seckill-system）

## 项目简介
基于 `Spring Boot 3.1` 的秒杀示例项目，覆盖登录注册、商品查询、秒杀下单、订单查询、缓存与可选搜索能力。

## 主要能力
- 用户注册/登录：`/api/users/*`
- 商品与秒杀：`/api/products/*`
- 订单查询：`/api/orders/*`
- 启动健康检查：`/api/health/init-status`
- 可选搜索：`/api/search/*`（Elasticsearch）

## 技术栈
- Java 17
- Spring Boot 3.1.8
- MyBatis-Plus
- dynamic-datasource
- MySQL、Redis、Kafka
- Elasticsearch（可选）

## 快速启动

### 1) 准备环境
- JDK 17+
- Maven 3.6+
- MySQL 8+
- Redis 6+
- Elasticsearch 7.x+（可选）

### 2) 创建数据库
```sql
CREATE DATABASE IF NOT EXISTS seckill_db DEFAULT CHARSET utf8mb4;
```

应用启动时会自动初始化表和基础商品数据。

### 3) 本地运行
```bash
mvn clean package -DskipTests
java -jar target/seckill-system-0.0.1-SNAPSHOT.jar
```

默认访问地址：`http://localhost:8083`

### 4) Docker Compose（可选）
```bash
docker-compose up -d
```

`docker-compose.yml` 提供 MySQL 主从、Redis、Elasticsearch 和应用容器示例。

## 常用接口
- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/products/list`
- `GET /api/products/{id}`
- `POST /api/products/seckill/{id}?userId={userId}`
- `GET /api/orders/{orderId}`
- `GET /api/orders/user`
- `GET /api/health/init-status`

统一返回结构：
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

## 测试
```bash
mvn test -Dtest=CacheServiceTest
mvn test -Dtest=ReadWriteSeparationTest
mvn test -Dtest=SearchServiceTest
```

## 前端页面
静态页面位于 `src/main/resources/static`：
- `/index.html`
- `/main.html`
- `/products.html`
- `/orders.html`
