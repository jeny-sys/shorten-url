# ADR-002 异步统计:Redis Stream

## 状态
Accepted (2026-06)

## 上下文
跳转接口要快(P99 < 10ms),不能同步写访问统计;需要一个轻量的"事件队列"承接访问事件,后台异步聚合。

## 候选方案
1. **Redis Stream**:Redis 内置队列,支持 Consumer Group、ACK、持久化
2. **Kafka**:大数据生态,功能最全
3. **Spring `@Async` + 内存阻塞队列**:零外部依赖

## 决策
**采用 Redis Stream**。

## 理由
- **边际成本零**:项目已用 Redis 做缓存,直接用 Stream 不引新组件
- **可靠性足够**:Consumer Group + ACK 保证不丢
- **学习曲线友好**:在已掌握 Redis 基本命令后,Stream 学习成本约 1 小时
- **简历卖点**:"用过消息队列"(中间件经验)

## 代价
- Stream 在极高吞吐场景下不如 Kafka
- Redis 故障时统计链路同时挂

## 后续方向
- 流量爆涨时迁移到 Kafka / Pulsar
