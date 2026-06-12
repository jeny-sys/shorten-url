# ShortenURL — 短链服务设计文档

| 字段 | 值 |
|------|-----|
| 文档日期 | 2026-06-12 |
| 项目代号 | ShortenURL |
| 作者人设 | 应届 / 在校生(具备 Spring Boot CRUD 基础,首次使用 Redis 与消息队列) |
| 目标用途 | 求职 / 实习简历项目(Java 后端 + 运维方向) |
| 预计周期 | 3 周(21 天,约 4–6 小时/天) |
| 状态 | 设计已定稿,等待评审 → 进入实现规划 |

---

## 1. 项目概述

**ShortenURL** 是一个轻量级的短链生成与跳转服务,作为简历项目展示后端工程能力与 DevOps 工程化实践。

输入一个长 URL,服务返回一个短 URL(如 `https://s.io/3D7`);访问者打开短 URL 时,服务进行 HTTP 302 跳转回原长 URL,同时异步记录访问统计。

项目的简历定位是:**单一业务、技术深度密集、完整的可观测性 + 容器化交付链路。** 业务实体仅 1–2 张表,但围绕缓存、异步化、限流、监控、CI/CD 形成完整工程体系。

---

## 2. 目标与非目标

### 2.1 目标(Goals)

| 编号 | 目标 | 度量方式 |
|------|------|---------|
| G1 | 实现可工作的短链生成、跳转、统计 3 个核心接口 | Postman / curl 可端到端验证 |
| G2 | 引入 Redis 作为热点缓存,降低数据库压力 | wrk 压测下命中率 > 95% |
| G3 | 异步统计链路与主跳转链路解耦 | 跳转接口 P99 < 10ms |
| G4 | 关键接口具备限流能力 | 超出阈值返回 HTTP 429 |
| G5 | 完整容器化:一行命令启动应用 + 依赖 + 监控 | `docker compose up` 起 5 个服务 |
| G6 | GitHub Actions 自动化测试与镜像构建 | push 触发 CI,产物归档至 GHCR |
| G7 | Prometheus + Grafana 可观测大盘 | QPS、P99 延迟、缓存命中率、JVM 指标可视化 |
| G8 | 简历素材完备 | 3 行项目描述 + 5 个面试 Q&A + 真实压测截图 |

### 2.2 非目标(Non-Goals,本期不做)

| 编号 | 非目标 | 不做的原因 |
|------|--------|------------|
| NG1 | Kubernetes 部署 | 学习曲线陡;Docker Compose 足以讲清"容器化"故事 |
| NG2 | 多机集群 / 高可用 | 单机讲清楚优于多机讲不清 |
| NG3 | ELK 日志栈 | Spring Boot 结构化日志已够;ELK 学习成本高 |
| NG4 | 完整管理后台(React/Vue) | 一页静态 HTML 足够演示;不分散精力到前端 |
| NG5 | Snowflake 分布式 ID | 单机场景 MySQL 自增即可,引入即过度设计 |
| NG6 | HTTPS / JWT / 用户体系 | 短链服务天然偏开放,登录/授权非核心 |
| NG7 | 多语言 / 国际化 | 个人简历项目不需要 |
| NG8 | 实时告警推送(钉钉/企业微信) | Grafana 自带告警可视化已够;不扩散范围 |

非目标条目并非"做不到",而是**主动选择不做**,在 README 的 "Trade-offs / Future Work" 章节中做明确说明,以体现取舍能力。

---

## 3. 用户与场景

### 3.1 主要用户角色

| 角色 | 描述 | 主要操作 |
|------|------|----------|
| 短链创建者 | 想把长链变短分享出去的人 | 调用生成接口、查看统计 |
| 短链访问者 | 通过短链跳转到长链的人 | HTTP GET 短链 |
| 简历评审人 / 面试官 | 拉项目仓库验证 | `docker compose up` 一键运行、看 Grafana 大盘 |

### 3.2 核心使用场景

- **S1 生成短链**:创建者提交长 URL → 拿到形如 `https://s.io/3D7` 的短链。
- **S2 跳转**:访问者打开短链 → 服务 302 跳转回原长链;耗时尽可能低。
- **S3 查看统计**:创建者查询某短链总访问量、最近 7 日趋势。

---

## 4. 架构设计

### 4.1 组件全景

```
                  ┌────────────────────┐
                  │   浏览器 / 客户端    │
                  │  (curl / Postman / │
                  │  一页演示 HTML)     │
                  └─────────┬──────────┘
                            │ HTTP
                            ▼
        ┌─────────────────────────────────────┐
        │      Spring Boot 应用                 │
        │  /api/shorten   生成短链              │
        │  /{shortCode}   跳转                  │
        │  /api/stats/.   读取统计              │
        │  内置:限流 + 异步统计投递             │
        └────┬──────────────────┬─────────────┘
             │                  │
             │ 短链映射读写       │ 访问事件投递(异步)
             ▼                  ▼
       ┌──────────┐         ┌──────────────┐
       │  Redis   │         │ Redis Stream │ ← 轻量消息队列
       │ (缓存)    │         └──────┬───────┘
       └────┬─────┘                │
            │                       │ 消费
            ▼                       ▼
       ┌──────────┐         ┌────────────────┐
       │  MySQL   │         │ 统计聚合 Worker │
       │ (持久化)  │ ◀────── │ (@Scheduled)   │
       └──────────┘ 批量回写  └────────────────┘

  外围:Docker Compose 编排 / GitHub Actions CI / Prometheus + Grafana 监控
```

### 4.2 组件职责

| 组件 | 职责 | 部署形态 |
|------|------|---------|
| Spring Boot 应用 | HTTP 入口、业务编排、缓存策略、异步投递 | Docker 容器 |
| MySQL 8 | 短链映射持久化存储(单表) | Docker 容器 |
| Redis 7 | 双重用途:① 热点短链缓存 ② Stream 充当消息队列 | Docker 容器 |
| 统计 Worker | 与 Spring Boot 应用同进程,`@Scheduled` 拉 Stream | 不独立部署 |
| Prometheus | 每 15 秒抓取 `/actuator/prometheus` | Docker 容器 |
| Grafana | 读取 Prometheus 数据画大盘 | Docker 容器 |

### 4.3 模块边界(Spring Boot 内部分层)

```
controller/         ← 只做参数校验 + 协议转换,不写业务逻辑
   ↓
service/            ← 业务核心,聚合 repository + infrastructure
   ↓
repository/         ← Spring Data JPA / MyBatis,只做数据访问
   ↓
infrastructure/     ← 与外部技术耦合的工具类(Base62 编码、限流、Redis Stream 投递)
config/             ← Spring 配置类
```

边界原则:**Controller 不直接调 Repository;Service 不直接 new Redis 客户端,都通过 infrastructure 层封装。** 这是 DDD/六边形架构的轻量化体现。

---

## 5. 数据模型

### 5.1 MySQL 表设计

#### 表 `url_map`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT AUTO_INCREMENT PK | 自增主键,也是短码的"数字身份" |
| `short_code` | VARCHAR(16) UNIQUE | Base62 编码后的短码,如 "3D7" |
| `long_url` | VARCHAR(2048) NOT NULL | 原始长 URL |
| `created_at` | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `expires_at` | DATETIME NULL | 过期时间(可选,本期默认不过期) |
| `total_clicks` | BIGINT DEFAULT 0 | 累计访问次数(批量回写,非实时) |

索引:
- 主键 `id`
- 唯一索引 `uk_short_code(short_code)` — 跳转接口查询的关键

### 5.2 Redis 键空间设计

| 键模式 | 类型 | 用途 | TTL |
|--------|------|------|-----|
| `url:{shortCode}` | STRING | 短链 → 长链映射缓存 | 24h + 随机 0–600s 偏移(防雪崩) |
| `null:{shortCode}` | STRING | 空值缓存,防穿透 | 5 分钟 |
| `stats:{shortCode}` | STRING(数值) | 累计点击计数器 | 永久(由 Worker 维护) |
| `stats:{shortCode}:day:{yyyyMMdd}` | STRING(数值) | 按天计数 | 30 天 |
| `clicks-stream` | STREAM | 访问事件队列(Consumer Group: `stats-worker`) | — |

> 限流状态不进 Redis —— 见 ADR-004,本期采用进程内令牌桶。

---

## 6. 核心业务流程

### 6.1 场景 A:生成短链(`POST /api/shorten`)

1. Controller 校验请求体:URL 非空、长度上限、协议合法(http/https)。
2. Service 通过 MySQL `INSERT` 拿到自增 `id`(如 12345)。
3. `Base62Encoder.encode(12345)` → "3D7"。
4. `UPDATE` 写回 `short_code` 字段(或在 INSERT 时使用单事务两步)。
5. 顺手把 `url:3D7 -> 长URL` 写入 Redis(预热)。
6. 返回 `{ "shortUrl": "https://s.io/3D7" }`。

**注意点:** 生成短码采用"先 INSERT 拿 ID,再编码回填"的两步;实现时使用单一事务保证原子性。

### 6.2 场景 B:跳转(`GET /{shortCode}`)

1. 限流检查(Bucket4j,基于 IP):未通过 → HTTP 429。
2. Redis `GET url:3D7`:
   - 命中 → 直接 302 跳转。
   - 命中 `null:3D7`(空值缓存) → HTTP 404,无需查 DB。
   - 未命中 → 继续。
3. MySQL `SELECT long_url WHERE short_code = '3D7'`:
   - 找到 → 写回 `url:3D7`(TTL 24h + 随机偏移) → 302 跳转。
   - 未找到 → 写 `null:3D7`(TTL 5 分钟) → HTTP 404。
4. **无论成功失败**,向 Redis Stream `clicks-stream` 投递一条访问事件(`XADD`)。
5. **投递不阻塞 302 响应**:在 302 写回响应后再 `XADD`,或使用 `CompletableFuture.runAsync`。

### 6.3 场景 C:统计 Worker(后台异步)

1. 同一 Spring Boot 进程内,`@Scheduled(fixedDelay = 1000)` 任务每秒拉一次 Stream。
2. `XREAD COUNT 100 STREAMS clicks-stream $`(从上次位置读最多 100 条)。
3. 对每条事件:
   - `INCR stats:{shortCode}`
   - `INCR stats:{shortCode}:day:{今天日期}`
4. 每 60 秒批量把 Redis 计数器同步回 MySQL `total_clicks`(降低 DB 写压力)。
5. 已消费的消息使用 Consumer Group 机制确认(`XACK`),保证重启不丢。

### 6.4 场景 D:查询统计(`GET /api/stats/{shortCode}`)

1. 直接读 `stats:{shortCode}` 和过去 7 天的 `stats:{shortCode}:day:*`。
2. 返回 `{ totalClicks, last7Days: [{date, clicks}] }`。

---

## 7. 关键技术决策(ADR 风格)

### ADR-001:短码生成方案 — Base62 + MySQL 自增 ID

**候选方案:**
- ✅ **Base62 + 自增 ID**:简单、无碰撞、依赖 DB 发号
- ❌ Snowflake:高性能、但本场景过度设计且学习曲线陡
- ❌ MD5/SHA Hash 截断:存在碰撞,需额外处理

**决策:** 选 Base62 + 自增 ID。
**理由:** 本项目目标 QPS 在万级别,MySQL 自增完全够用;开发周期紧,Snowflake 额外耗时 ≥ 3 天;Hash 截断需要冲突处理逻辑,增加复杂度且对简历亮点贡献有限。

### ADR-002:异步统计 — Redis Stream

**候选方案:**
- ✅ **Redis Stream**:既已用 Redis,边际成本低;支持 Consumer Group、消息持久化
- ❌ Kafka:学习曲线陡,运维复杂,与项目规模不匹配
- ❌ Spring `@Async` + 内存阻塞队列:进程崩溃即丢,无重放能力

**决策:** 选 Redis Stream。
**理由:** 在学习成本与功能之间取最佳平衡点;同时保留"中间件经验"简历亮点;若学习受阻可退化到 `@Async` 方案(已列入风险预案)。

### ADR-003:不采用 Kubernetes

**候选方案:**
- ✅ **Docker Compose**:一行命令启动,符合"单机演示型项目"定位
- ❌ Kubernetes:学习曲线陡,本期 21 天日程不允许

**决策:** Docker Compose,K8s 列入 Future Work。
**理由:** 项目核心简历卖点为业务深度 + 可观测性,K8s 在单机演示中无价值且会挤压核心模块时间。

### ADR-004:限流方案 — Bucket4j 本地实现

**候选方案:**
- ✅ **Bucket4j(令牌桶)**:Java 库,几行代码集成,支持 Redis 后端做分布式
- ❌ Spring Cloud Gateway:引入额外组件,与单体架构不匹配
- ❌ Sentinel:学习成本高,功能溢出

**决策:** Bucket4j **进程内**令牌桶,桶状态由 `ConcurrentHashMap<IP, Bucket>` 持有(单实例,不依赖 Redis)。
**理由:** 接入成本低、能讲原理、面试常考令牌桶。多实例时再迁移到 Bucket4j-Redis 后端,列入 Future Work。

### ADR-005:集成测试 — Testcontainers

**候选方案:**
- ✅ **Testcontainers**:CI 自动起真实 MySQL/Redis 容器,行为与生产一致
- ❌ H2 / Embedded Redis:行为差异(如不支持 Stream),失去测试价值

**决策:** Testcontainers。
**理由:** 简历亮点(行为还原度高);与 Docker 化主线技术一致。

---

## 8. 运维层设计

### 8.1 Docker 化

- **`Dockerfile`**:基于 `eclipse-temurin:17-jre-alpine`,multi-stage build 减小镜像体积。
- **`docker-compose.yml`** 编排 5 个服务:`app`、`mysql`、`redis`、`prometheus`、`grafana`。
- 应用与 MySQL/Redis 通过 Compose 网络互通,使用服务名而非 localhost 连接。
- 配置文件分离:`application.yml`(本地开发)、`application-docker.yml`(容器环境)。

### 8.2 GitHub Actions CI

`.github/workflows/ci.yml` 触发条件:`push` 到 main / 任何 PR。流水线步骤:

1. Checkout 代码
2. Setup JDK 17 + Maven 缓存
3. `mvn test`(失败立即中断)
4. `mvn package -DskipTests`
5. Docker build → tag 为 `ghcr.io/<user>/shorten-url:<sha>` 和 `:latest`
6. Login GHCR → push 镜像

README 顶部展示 CI 徽章(`[![CI](badge-url)](workflow-url)`)。

### 8.3 监控大盘

**指标暴露:** Spring Boot 通过 `spring-boot-starter-actuator` + `micrometer-registry-prometheus` 暴露 `/actuator/prometheus`。

**Prometheus 配置:** 每 15 秒抓一次 `app:8080/actuator/prometheus`。

**Grafana 大盘内容:**
| 面板 | 数据源指标 | 简历用途 |
|------|----------|----------|
| QPS 曲线 | `rate(http_server_requests_seconds_count[1m])` | 写"支持 X k QPS" |
| P99 延迟 | `histogram_quantile(0.99, ...)` | 写"P99 < 10ms" |
| Redis 命中率 | 自定义 `cache_hit_total` / `cache_total` | 写"命中率 > 99%" |
| Stream 积压 | `redis_stream_length{stream="clicks-stream"}` | 证明 Worker 跟得上 |
| JVM 内存 / GC | Micrometer 默认指标 | 证明无内存泄漏 |

Grafana provisioning 通过挂载 JSON 文件自动加载大盘,避免每次手工配置。

---

## 9. 测试策略

### 9.1 必做测试

| 类型 | 范围 | 工具 |
|------|------|------|
| 单元测试 | `Base62Encoder` 编解码 | JUnit 5 |
| 单元测试 | `ShortenService` 主流程(Mock Repository) | JUnit 5 + Mockito |
| 集成测试 | 端到端"生成 → 跳转"流程 | `@SpringBootTest` + Testcontainers(真 MySQL + 真 Redis) |
| CI 自动跑 | 所有上述测试 | GitHub Actions |

**目标:** 至少 8 个测试用例;CI 失败阻塞合入。

### 9.2 不做的测试

- Controller mock 测试(收益低)
- E2E 浏览器自动化(超出范围)
- 100% 覆盖率(关键路径有就行)

### 9.3 压测

- 工具:`wrk`(Mac/Linux 原生;Windows 用 Docker 镜像 `williamyeh/wrk`)
- 压测命令模板:`wrk -t12 -c400 -d30s --latency http://localhost:8080/{shortCode}`
  - `-t12` 12 个线程,`-c400` 400 个并发连接,`-d30s` 持续 30 秒
- 记录数据:QPS、延迟分布(Avg / P50 / P95 / P99)、缓存命中率
- 同时截图 Grafana 大盘,做为简历数据来源

---

## 10. 时间表与里程碑

### Week 1:核心打通

| 天 | 任务 | 里程碑 |
|----|------|--------|
| D1 | 项目骨架 + MySQL 表 + 配置 | 应用能起,DB 连通 |
| D2 | 生成短链接口(纯 MySQL) | Postman 调通 |
| D3 | 跳转接口(纯 MySQL) | 端到端跑通 |
| D4 | 一页演示 HTML | 浏览器可演示 |
| D5 | Redis 学习 + 缓存接入(查询侧) | 缓存命中流程跑通 |
| D6 | 缓存回填 + 空值缓存 + 防雪崩 | Cache-Aside 完成 |
| D7 | **缓冲日** | 收尾 + 准备 Week 2 |

### Week 2:亮点 + 容器化 + CI

| 天 | 任务 | 里程碑 |
|----|------|--------|
| D8 | Redis Stream 学习 + 投递 | 跳转后投递事件 |
| D9 | Worker 消费 + 统计接口 | 统计闭环 |
| D10 | Bucket4j 限流接入 | 超出阈值返回 429 |
| D11 | Dockerfile | 应用容器化 |
| D12 | docker-compose.yml(先编排 app + mysql + redis;Prometheus/Grafana 在 D15 追加) | 一行启动核心服务 |
| D13 | GitHub Actions CI | push 自动跑测试 + 镜像构建 |
| D14 | **缓冲日** | 收尾 |

### Week 3:监控 + 压测 + 简历素材

| 天 | 任务 | 里程碑 |
|----|------|--------|
| D15 | Prometheus + Grafana 接入 docker-compose | 大盘可访问 |
| D16 | 自定义 3 个面板 + 套 JVM 模板 | 大盘"上简历水准" |
| D17 | wrk 压测 + 截图 | 数据落袋 |
| D18 | 测试补全(单测 + Testcontainers 集测) | CI 全绿 |
| D19 | README + 架构图(Mermaid) + ADR | 文档完备 |
| D20 | 简历版描述 + 面试 Q&A 自答稿 | 简历素材包齐 |
| D21 | **总缓冲日** | 收官 |

---

## 11. 风险与缓解

| 风险 | 影响 | 缓解 / 退路 |
|------|------|-----------|
| Week 1 拖延,后续雪崩 | 关键功能砍掉 | D7/D14/D21 缓冲日;最坏时砍 Bucket4j |
| Redis Stream 学不动 | 异步链路无法落地 | 退路:用 Spring `@Async` + 内存队列;简历改写为"基于 @Async 的异步链路" |
| GitHub Actions 卡住 | CI 无法落地 | 退路:直接套模板 yml;不实现 GHCR push,只跑测试 |
| Grafana 大盘画不出 | 监控亮点缺失 | 退路:用 grafana.com 上现成的 Spring Boot 模板 ID(直接 import) |
| 时间真不够 | 简历亮点缩水 | 优先级砍序:① Bucket4j → ② Testcontainers → ③ GHCR push → ④ 自定义 Grafana 面板 |

---

## 12. 交付物清单(项目"完成"的定义)

### 代码相关
- [ ] 代码 push 至 GitHub 公开仓库,URL 可直接放简历
- [ ] `main` 分支 CI 显示绿色徽章
- [ ] 至少 8 个单元 + 集成测试,CI 全通过
- [ ] Service 不超 200 行,Controller 不写业务

### 文档相关
- [ ] README:5 秒能让面试官明白项目干啥(顶部架构图 + 一句话定位)
- [ ] README "一行命令运行"章节(`git clone && docker compose up`)
- [ ] 3–5 条 ADR 文档(放 `docs/decisions/`)
- [ ] 真实截图:Grafana 大盘 + wrk 压测结果(放 `docs/screenshots/`)

### 可演示物
- [ ] 本地 `docker compose up` 起完整系统
- [ ] 演示页能浏览器访问,提交+跳转能演示
- [ ] Grafana 大盘可在面试时打开
- [ ] wrk 压测能现场重新跑,数据可复现

### 简历素材
- [ ] 3 行简历版项目描述
- [ ] 5 个面试 Q&A 自答稿
- [ ] 1 个 60 秒口头讲解版本(练熟)

---

## 13. 简历素材

### 13.1 项目描述模板

```
ShortenURL — 高性能短链服务                                2026.X — 2026.X
个人项目 · Java 17 · Spring Boot 3 · Redis · MySQL · Docker
GitHub: github.com/<yourname>/shorten-url  [CI passing]

• 实现短链生成与跳转服务,基于 Base62 + 自增 ID 保证短码唯一性;
  接入 Redis 缓存使数据库查询量降低 90%+,跳转 P99 < 10ms(wrk 实测)。
• 异步统计链路基于 Redis Stream,主跳转链路与统计计算解耦;
  接入 Bucket4j 完成接口级令牌桶限流。
• Docker Compose 一键编排应用、MySQL、Redis 与监控组件;
  GitHub Actions 实现自动化测试与镜像构建;
  Prometheus + Grafana 可视化 QPS、P99 延迟、缓存命中率、JVM 指标。
```

### 13.2 面试 Q&A 自答提纲

| 问题 | 回答主线 |
|------|----------|
| 为什么选 Base62 不选 Snowflake? | 开发周期权衡;场景下 MySQL 自增够用,Snowflake 是过度设计。 |
| 缓存击穿 / 雪崩 / 穿透怎么解? | 击穿:热点不过期;雪崩:TTL 随机偏移;穿透:空值缓存。 |
| Redis Stream 与 Kafka 区别? | Stream 是 Redis 内置轻量队列,适中小流量;Kafka 是大数据生态。 |
| P99 怎么测? | wrk 压测 + Grafana histogram_quantile;数据可复现。 |
| 扩展到 10x 流量改什么? | 发号换 Snowflake;Redis 集群;K8s 多副本;DB 读写分离。 |

---

## 14. 未来扩展(Future Work,写在 README)

- 引入 Snowflake 分布式 ID,支持多实例水平扩展
- 接入 Kubernetes 部署,使用 Helm Chart 管理
- 增加 ELK 日志栈,集中收集与查询
- 增加企业微信 / 钉钉告警推送
- 增加用户体系与 JWT 鉴权,支持私有短链
- 增加 CDN 集成,边缘节点直接处理跳转

每一条都对应"为什么本期不做"的反面,在面试中可灵活援引,展示长期视角。

---

## 15. 设计文档结束

本设计经过 5 段对话评审,作者已逐段确认。下一步:
1. 由用户做整体文档评审
2. 通过后,使用 `superpowers:writing-plans` 技能编写实现计划
3. 进入实现阶段时,遵守 `superpowers:test-driven-development` 等技能

