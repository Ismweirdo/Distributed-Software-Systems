# 商品秒杀系统 JMeter 测试指南

本文档基于当前代码接口，给出可直接复用的 JMeter 压测方案。

## 1. 测试前准备

### 1.1 启动依赖
- MySQL
- Redis
- （可选）Elasticsearch

### 1.2 启动应用
可选两种方式：

- 单实例：`http://localhost:8083`
- 双实例 + Nginx：`http://localhost`（Nginx 转发到 8081/8082）

建议：
- 测后端接口本身时，直接打 `8083`。
- 测负载均衡与动静分离时，打 Nginx `80` 端口。

## 2. 当前可测接口清单

### 用户接口
- `POST /api/users/register`
- `POST /api/users/login`

### 商品接口
- `GET /api/products/list`
- `GET /api/products/{id}`
- `POST /api/products/seckill/{id}`

### 搜索接口（可选）
- `GET /api/search/keyword?keyword=手机&page=0&size=10`
- `GET /api/search/name?name=iPhone&page=0&size=10`
- `GET /api/search/price?minPrice=1000&maxPrice=5000&page=0&size=10`
- `POST /api/search/sync`

## 3. JMeter 测试计划建议结构

```text
Seckill Test Plan
├── Thread Group - 商品列表
│   ├── HTTP Request Defaults
│   ├── HTTP Header Manager
│   ├── HTTP Request (GET /api/products/list)
│   └── Aggregate Report
├── Thread Group - 商品详情缓存
│   ├── HTTP Request (GET /api/products/1)
│   └── Summary Report
├── Thread Group - 秒杀接口
│   ├── HTTP Request (POST /api/products/seckill/1)
│   └── Aggregate Report
└── Thread Group - Nginx 静态资源（可选）
    ├── HTTP Request (GET /index.html)
    ├── HTTP Request (GET /main.html)
    └── HTTP Request (GET /products.html)
```

## 4. 场景 A：商品列表吞吐测试

### 4.1 配置建议
- Threads: 100
- Ramp-Up: 10s
- Duration: 60s
- Method: `GET`
- Path: `/api/products/list`

### 4.2 观察指标
- 平均响应时间
- 吞吐量（TPS/QPS）
- 错误率

## 5. 场景 B：商品详情缓存命中测试

### 5.1 目标
验证 `GET /api/products/{id}` 在缓存预热前后的响应差异。

### 5.2 操作建议
1. 先用小并发请求 `GET /api/products/1` 预热。
2. 再用较高并发重复请求同一个 ID。
3. 对比两轮平均响应时间。

### 5.3 期望
- 预热后响应时间明显下降。
- 错误率保持较低。

## 6. 场景 C：秒杀接口并发扣减测试

### 6.1 配置建议
- Threads: 50
- Ramp-Up: 5s
- Loop Count: 10
- Method: `POST`
- Path: `/api/products/seckill/1`

### 6.2 观察点
- 成功/失败响应比例
- 是否出现超卖（库存负数）
- 秒杀后商品详情接口返回库存是否持续下降

## 7. 场景 D：Nginx 负载均衡测试（可选）

### 7.1 前提
- 已启动 `instance1`（8081）和 `instance2`（8082）
- Nginx `location /api/` 已代理到 `seckill-backend-roundrobin`

### 7.2 压测接口
- `GET http://localhost/api/products/list`

### 7.3 验证方式
- 查看响应头 `X-Upstream-Server` 分布
- 查看两个实例日志，确认请求分担

## 8. 场景 E：Nginx 动静分离测试（可选）

压测静态资源：
- `/index.html`
- `/main.html`
- `/products.html`

观察点：
- 静态资源响应时间通常应低于动态接口
- 响应头可观察 `X-Cache-Status`（取决于 Nginx 对应 location）

## 9. 搜索接口压测提示

默认 Elasticsearch 关闭时，搜索接口会返回空分页，这属于预期。
如需测试真实搜索性能，请先：

1. 启用 `elasticsearch.enabled=true`
2. 启动 ES 并保证索引可写
3. 调用 `POST /api/search/sync` 完成同步

## 10. 结果记录模板

| 场景 | 并发 | 持续时间 | 平均响应(ms) | TPS | 错误率 |
|---|---:|---:|---:|---:|---:|
| 商品列表 |  |  |  |  |  |
| 商品详情缓存 |  |  |  |  |  |
| 秒杀接口 |  |  |  |  |  |
| Nginx 负载均衡 |  |  |  |  |  |
| Nginx 静态资源 |  |  |  |  |  |

## 11. 常见问题

### 11.1 出现大量连接失败
优先检查：服务是否已启动、端口是否正确、JMeter 目标域名是否可达。

### 11.2 秒杀接口全失败
检查测试商品库存是否已耗尽；可在 MySQL 中重置库存后重测。

### 11.3 搜索接口始终空结果
默认 ES 关闭，需先启用并同步数据。
