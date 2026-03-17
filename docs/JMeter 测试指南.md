# ==================== 商品秒杀系统 - JMeter 压力测试指南 ====================

## 一、环境准备

### 1.1 软件要求
- Apache JMeter 5.4+ 
- MySQL 8.0+
- Redis 7.0+
- Nginx 1.20+
- Java 17+

### 1.2 启动服务

#### 1. 启动数据库和 Redis
```bash
docker-compose up -d mysql redis
```

#### 2. 初始化数据库
```bash
# 执行 SQL 脚本
mysql -u root -p seckill_db < src/main/resources/db/init_seckill_product.sql
```

#### 3. 启动两个后端实例

**实例 1（端口 8081）:**
```bash
java -jar target/seckill-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=instance1
```

**实例 2（端口 8082）:**
```bash
java -jar target/seckill-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=instance2
```

#### 4. 启动 Nginx
```bash
# Windows: 下载 Nginx for Windows，解压后运行
nginx.exe

# Linux/Mac:
sudo nginx -c /path/to/nginx.conf
```

---

## 二、JMeter 测试脚本说明

### 2.1 测试计划结构

```
商品秒杀系统测试计划
├── 线程组 - 负载均衡测试
│   ├── HTTP 请求 - 获取商品列表
│   └── 查看结果树
├── 线程组 - 商品详情缓存测试
│   ├── HTTP 请求 - 获取商品详情（带缓存）
│   └── 聚合报告
├── 线程组 - 动静分离测试
│   ├── HTTP 请求 - 静态 HTML 页面
│   ├── HTTP 请求 - CSS 文件
│   ├── HTTP 请求 - JS 文件
│   └── 聚合报告
└── 线程组 - 秒杀接口压测
    ├── HTTP 请求 - 执行秒杀
    └── 聚合报告
```

### 2.2 创建测试脚本

#### 步骤 1: 添加线程组
右键 "Test Plan" → Add → Threads (Users) → Thread Group

配置示例:
- Number of Threads (users): 100
- Ramp-Up Period (in seconds): 10
- Loop Count: ∞ (勾选 Infinite)
- Duration (seconds): 60

#### 步骤 2: 添加 HTTP 请求默认值
右键 Thread Group → Add → Config Element → HTTP Request Defaults

配置:
- Server Name or IP: localhost
- Port: 80
- Protocol: http

#### 步骤 3: 添加 HTTP 信息头管理器
右键 Thread Group → Add → Config Element → HTTP Header Manager

添加:
- Content-Type: application/json

#### 步骤 4: 添加监听器
右键 Thread Group → Add → Listener → 选择以下监听器:
- View Results Tree (查看结果树)
- Summary Report (汇总报告)
- Aggregate Report (聚合报告)
- Graph Results (图形结果)

---

## 三、具体测试场景

### 3.1 负载均衡测试

**目的**: 验证 Nginx 将请求均匀分配到两个后端实例

#### 测试配置:
1. 创建线程组："负载均衡测试"
2. 添加 HTTP 请求:
   - Path: `/api/products/list`
   - Method: GET
3. 添加响应断言，验证返回 code=200

#### 观察指标:
- 查看两个后端实例的日志，确认请求数大致相等
- 检查响应头 `X-Upstream-Server`，确认请求被分发到不同服务器

#### 预期结果:
- 实例 1 和实例 2 的请求比例接近 1:1（轮询模式）
- 平均响应时间 < 200ms

---

### 3.2 动静分离测试

**目的**: 验证 Nginx 直接提供静态资源，不经过后端服务

#### 测试配置:
1. 创建线程组："动静分离测试"
2. 添加多个 HTTP 请求:
   
   **请求 1 - HTML 页面:**
   - Path: `/index.html`
   - 预期响应头：`X-Cache-Status: STATIC`
   
   **请求 2 - CSS 文件:**
   - Path: `/style.css` (如果有的话)
   - 预期响应头：`X-Cache-Status: STATIC`
   
   **请求 3 - JS 文件:**
   - Path: `/app.js` (如果有的话)
   - 预期响应头：`X-Cache-Status: STATIC`

#### 观察指标:
- 静态资源响应时间应 < 50ms
- 响应头包含 `X-Cache-Status: STATIC`
- 后端服务日志中不应该有静态资源访问记录

#### 预期结果:
- 静态资源响应时间显著快于动态 API
- Nginx 访问日志显示静态资源直接由 Nginx 提供

---

### 3.3 分布式缓存测试

**目的**: 验证 Redis 缓存效果，以及穿透/击穿/雪崩解决方案

#### 测试配置:
1. 创建线程组："缓存性能测试"
2. 添加 HTTP 请求:
   - Path: `/api/products/1`
   - Method: GET
3. 设置高并发：200 线程，Ramp-Up 5 秒

#### 测试步骤:

**第一次请求（缓存未命中）:**
```bash
curl http://localhost/api/products/1
```
观察后端日志：应该显示"缓存未命中，查询数据库"

**第二次请求（缓存命中）:**
```bash
curl http://localhost/api/products/1
```
观察后端日志：应该显示"缓存命中"

#### 压测观察指标:
- 首次请求响应时间：~100-200ms（数据库查询）
- 后续请求响应时间：~10-50ms（缓存读取）
- Redis 命中率：> 90%

#### 验证缓存穿透保护:
```bash
# 请求不存在的商品 ID（如 999999）
curl http://localhost/api/products/999999
```
观察日志：应该显示"布隆过滤器拦截"或"设置空值缓存"

---

### 3.4 秒杀接口压测

**目的**: 测试高并发下的库存扣减和缓存更新

#### 测试配置:
1. 创建线程组："秒杀接口压测"
2. 添加 HTTP 请求:
   - Path: `/api/products/seckill/1`
   - Method: POST
3. 配置:
   - Threads: 50
   - Ramp-Up: 5 秒
   - Loop Count: 10

#### 观察指标:
- 成功响应数 vs 失败响应数
- 平均响应时间
- 数据库库存数量是否正确扣减
- 缓存是否正确删除和重建

#### 预期结果:
- 不会出现超卖现象（库存不会变成负数）
- 缓存能够正确更新
- 响应时间随着缓存命中率提高而降低

---

## 四、不同负载均衡算法测试

### 4.1 修改 Nginx 配置测试不同算法

#### 1. 轮询（默认）
```nginx
location /api/ {
    proxy_pass http://seckill-backend-roundrobin;
}
```

#### 2. 权重配置
```nginx
location /api/ {
    proxy_pass http://seckill-backend-weighted;
}
```
预期：实例 1 处理 75% 请求，实例 2 处理 25%

#### 3. IP Hash
```nginx
location /api/ {
    proxy_pass http://seckill-backend-iphash;
}
```
预期：同一 IP 的请求总是到同一台服务器

#### 4. 最少连接
```nginx
location /api/ {
    proxy_pass http://seckill-backend-leastconn;
}
```
预期：请求优先到当前连接数最少的服务器

---

## 五、监控与结果分析

### 5.1 关键指标

#### 响应时间
- 优秀：< 100ms
- 良好：100-500ms
- 一般：500ms-1s
- 差：> 1s

#### 吞吐量 (TPS/QPS)
计算公式：
```
TPS = 总请求数 / 测试时间
```

#### 错误率
```
错误率 = (失败请求数 / 总请求数) × 100%
```
目标：< 0.1%

### 5.2 使用 JMeter 插件增强监控

安装插件管理器：
https://jmeter-plugins.org/install/Install/

推荐插件:
- PerfMon Metrics Collector - 监控服务器资源
- Response Times Over Time - 响应时间趋势图
- Active Threads Over Time - 活跃线程数

### 5.3 查看后端日志

**实例 1 日志:**
```bash
tail -f logs/instance1.log | grep "缓存"
```

**实例 2 日志:**
```bash
tail -f logs/instance2.log | grep "缓存"
```

**Nginx 访问日志:**
```bash
tail -f /var/log/nginx/access.log
```

## 七、测试报告

### 测试环境
- CPU: 
- 内存: 
- 网络: 
- 后端实例数：2
- Nginx 版本: 

### 测试场景
1. 负载均衡 - 轮询算法
2. 动静分离 - 静态资源访问
3. 缓存性能 - 商品详情查询
4. 高并发 - 秒杀接口

### 测试结果汇总

| 测试场景 | 并发数 | 平均响应时间 | TPS | 错误率 |
|---------|--------|-------------|-----|--------|
| 负载均衡 | 100 | 150ms | 650 | 0% |
| 动静分离 | 200 | 25ms | 2800 | 0% |
| 缓存查询 | 200 | 45ms | 1900 | 0% |
| 秒杀接口 | 50 | 280ms | 175 | 2% |
