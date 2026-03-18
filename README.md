# 秒杀系统（seckill-system）

## 项目简介
本项目是基于 `Spring Boot 3.1` 的秒杀示例系统，包含以下能力：

- 用户注册与登录（`/api/users/*`）
- 商品列表、商品详情、秒杀下单（`/api/products/*`）
- Redis 缓存（空值缓存、互斥锁、随机 TTL）
- 可选的 Elasticsearch 搜索（`/api/search/*`）
- Nginx 负载均衡与动静分离示例配置（`nginx.conf`）

## 当前代码状态（和文档强相关）
- 默认启动端口是 `8083`（`src/main/resources/application.yml`）。
- `instance1`/`instance2` profile 分别使用 `8081` 和 `8082`。
- Elasticsearch 默认关闭：`elasticsearch.enabled=false`。
- 读写分离注解与切面已实现（`@DataSourceSlave` + `DataSourceAspect`），但默认配置文件里没有 `spring.datasource.dynamic.*` 的主从数据源定义，需要手动补齐后才会真正按主从路由。
- 初始化 SQL 仅包含 `seckill_product` 表，`user` 表需要手动创建（下方已给 SQL）。

## 技术栈
- Java 17
- Spring Boot 3.1.8
- MyBatis-Plus 3.5.6
- dynamic-datasource 4.2.0
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
先创建数据库并导入商品表：

```sql
CREATE DATABASE IF NOT EXISTS seckill_db DEFAULT CHARSET utf8mb4;
USE seckill_db;
SOURCE D:/CreatorProject/seckill-system/src/main/resources/db/init_seckill_product.sql;
```

再手动创建用户表（当前仓库未提供该表的 init 脚本）：

```sql
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
```

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

### 6. Docker Compose（可选）
`docker-compose.yml` 可直接拉起 MySQL/Redis/后端容器：

```bash
docker-compose up -d
```

注意：Compose 将 MySQL 映射为主机 `3307`，若你在主机直接运行后端，请同步修改 `spring.datasource.url` 端口为 `3307`。

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
- `POST /api/products/seckill/{id}`

示例：

```bash
curl http://localhost:8083/api/products/list
curl http://localhost:8083/api/products/1
curl -X POST http://localhost:8083/api/products/seckill/1
```

### 搜索接口（可选）
- `GET /api/search/keyword?keyword=手机&page=0&size=10`
- `GET /api/search/name?name=iPhone&page=0&size=10`
- `GET /api/search/price?minPrice=1000&maxPrice=5000&page=0&size=10`
- `POST /api/search/sync`

说明：默认 Elasticsearch 关闭，搜索接口会返回空分页结果，`/sync` 会直接跳过实际同步。

## 启用 Elasticsearch（可选）

### 1. 修改配置
在 `application.yml`（或自定义 profile）中启用：

```yaml
elasticsearch:
  enabled: true

spring:
  data:
    elasticsearch:
      repositories:
        enabled: true
      uris: http://127.0.0.1:9200
```

### 2. 启动 ES 并安装 IK（如需中文分词）

### 3. 触发同步

```bash
curl -X POST http://localhost:8083/api/search/sync
```

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

## 前端页面
静态页面位于 `src/main/resources/static`：
- `/index.html`
- `/main.html`
- `/products.html`

如使用 Nginx，请确保 `root` 指向该目录。
