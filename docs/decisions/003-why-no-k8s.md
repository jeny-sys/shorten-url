# ADR-003 不采用 Kubernetes

## 状态
Accepted (2026-06)

## 上下文
"运维方向"简历项目通常会被问"用过 K8s 吗?"。

## 决策
**不采用 K8s,使用 Docker Compose 单机编排**。

## 理由
- **学习曲线陡**:K8s 概念栈深(Pod / Service / Ingress / Deployment / ConfigMap / Volume),21 天项目无法覆盖
- **可演示性差**:单机演示型项目,K8s 的"调度 / 滚动升级 / 自愈"价值无法体现
- **挤压核心模块**:学 K8s 时间会挤掉缓存/监控等真正可演示的模块

## 代价
- 简历缺少 K8s 字眼

## 后续方向
- 后续学习 K8s 时可用本项目做实操载体:写 Helm Chart 重新部署,补 K8s 相关 ADR-006
