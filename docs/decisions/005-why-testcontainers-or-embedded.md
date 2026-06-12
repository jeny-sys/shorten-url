# ADR-005 集成测试:Testcontainers + Embedded Redis

## 状态
Accepted (2026-06)

## 上下文
集成测试需要 MySQL + Redis。选择假实现(H2 / EmbeddedRedis)还是真容器(Testcontainers)。

## 决策
**采用 Embedded Redis(测试期)+ H2(dev profile)+ MySQL 容器(生产 profile)** 的混合策略。

## 理由
- **MySQL 集成测试**:Testcontainers 启动真 MySQL,行为还原度高
- **Redis 集成测试**:embedded-redis 库,Java 进程内直接跑,无 Docker 依赖
- **本地开发**:H2 内存数据库 + Spring profile 切换,开发者无需任何外部依赖
- **CI 通用**:GitHub Actions runner 自带 Docker,本地与 CI 一致

## 代价
- embedded-redis 与真 Redis 在极端场景下有微小行为差异
- Testcontainers 单测速度慢(每次起容器约 5 秒)

## 后续方向
- 引入 Awaitility 处理异步断言(已在 StatsWorker 测试用上)
- 多实例 Redis 集群测试时切换到 Testcontainers Redis
