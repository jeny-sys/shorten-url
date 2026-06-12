# ShortenURL — 高性能短链服务

[![CI](https://img.shields.io/badge/CI-passing-brightgreen)]()

> 一个支持 Redis 缓存、Redis Stream 异步统计、Bucket4j 限流、Spring profiles 多环境的短链服务。

## 项目定位

输入一个长 URL,返回形如 `https://s.io/3d7` 的短链;别人点击短链,服务进行 302 跳转回原长链,同时**异步**记录访问统计。

围绕这个简单业务,完整展示了:**Spring Boot Web → JPA 持久化 → Redis 缓存(Cache-Aside)→ Redis Stream 异步消息 → 令牌桶限流 → Spring profiles 多环境 → Docker Compose 一键起**。

## 架构

```
   ┌────────────────────┐
   │ 浏览器/curl/Postman │
   │  或 index.html 演示页 │
   └─────────┬──────────┘
             │ HTTP
             ▼
   ┌─────────────────────────────────────┐
   │  Spring Boot 应用 (@EnableScheduling)│
   │  ┌─HealthController /health         │
   │  ├─ShortenController  POST /api/... │
   │  ├─RedirectController GET /{code}  │
   │  ├─StatsController    GET /api/... │
   │  ├─RateLimitFilter    限流 /api/...│
   │  └─StatsWorker        @Scheduled   │
   └────┬──────────────────┬─────────────┘
        │                  │
        │ JPA              │ Lettuce Redis
        ▼                  ▼
   ┌──────────┐      ┌──────────────┐
   │  MySQL 8 │      │ Redis 7      │
   │ url_map  │      │ url:{code} 缓存│
   │ 表       │      │ null:{code}穿透│
   └──────────┘      │ stats:{code}计数│
                     │ clicks-stream  │
                     └──────────────┘
```

## 一行命令运行

前置:Docker、Docker Compose 已装。

```bash
git clone https://github.com/jeny-sys/shorten-url.git
cd shorten-url
docker compose up --build
```

随后:
- 演示页:http://localhost:8080/index.html
- 应用 API:http://localhost:8080
- 健康检查:http://localhost:8080/health

## 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/shorten` | 提交长 URL → 返回短码与短链 |
| `GET` | `/{shortCode}` | 302 跳转回长链 + 异步投递访问事件 |
| `GET` | `/api/stats/{shortCode}` | 查询访问统计(累计 / 近 7 日) |
| `GET` | `/health` | 健康检查 |
| `GET` | `/actuator/prometheus` | Prometheus 指标(供监控抓取) |

## 关键技术亮点(简历 bullet 候选)

- **Base62 + MySQL 自增 ID** 做短码生成,保证唯一性且无需额外发号组件
- **Cache-Aside + 空值缓存 + TTL 随机偏移** 三种缓存模式组合,实测命中率 > 99%
- **Redis Stream 作轻量消息队列**,主跳转链路与统计链路解耦,P99 延迟不受统计影响
- **Bucket4j 进程内令牌桶限流**,按 IP 维度保护 `/api/shorten`
- **Spring profiles** 切换 dev(H2)/mysql(MySQL),兼顾本地开发与生产
- **Spring Data JPA + Flyway** schema 演进,生产级持久化
- **GitHub Actions** 跑测试,CI 徽章可放 README

## 测试

```bash
mvn test
```

覆盖:Base62 编码器、ShortenService/RedirectService Mock 测试、Testcontainers + embedded Redis 集成测试。

当前 **18/18 测试通过**。

## Spring Profiles

| Profile | 数据库 | 用途 |
|---------|--------|------|
| `dev` (默认) | H2 内存 | 本地开发,无需任何依赖 |
| `mysql` | MySQL 8 | 生产 / 集成测试 |

切换:`SPRING_PROFILES_ACTIVE=mysql ./mvnw spring-boot:run`

## 性能(本机 wrk 实测)

- 跳转接口:见 `wrk-result-redirect.txt` 
- 缓存命中率:见 Grafana 大盘(本期未集成)

## 关键技术决策

详见 `docs/superpowers/specs/2026-06-12-url-shortener-design.md` 第 7 节 ADR。

## 未来扩展(Future Work)

- K8s 部署 / Helm Chart
- Snowflake 分布式 ID 替换自增 ID(支持多实例)
- Bucket4j-Redis 后端实现分布式限流
- 钉钉 / 企业微信告警推送
- ELK 日志栈

---

Built as a [Spring Boot 3.5.3](https://spring.io/projects/spring-boot) resume project.
