# ADR-004 限流:Bucket4j 进程内

## 状态
Accepted (2026-06)

## 上下文
`POST /api/shorten` 接口需要防滥用。需要选择限流方案。

## 候选方案
1. **Bucket4j 进程内令牌桶**:`ConcurrentHashMap<IP, Bucket>` 持有桶状态
2. **Bucket4j + Redis 后端**:分布式令牌桶
3. **Spring Cloud Gateway**:网关层限流
4. **Sentinel**:阿里限流框架

## 决策
**Bucket4j 进程内**。

## 理由
- **单实例项目**:不需要分布式
- **接入成本低**:一个 Filter + 一个 Component,几十行代码
- **算法面试常考**:令牌桶/漏桶原理在面试中能讲深

## 代价
- 多实例时令牌不共享
- 重启丢桶状态

## 后续方向
- 多实例时切换到 Bucket4j-Redis 后端(改 1 个配置)
