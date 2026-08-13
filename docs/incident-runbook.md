# Incident Runbook

对照 Prometheus 规则见 [`deploy/observability/prometheus-alerts.yml`](../deploy/observability/prometheus-alerts.yml)。告警名与下文章节一一对应。本页不引入 Grafana/Alertmanager 部署；把规则导入现有 Prometheus 即可。

抓指标：管理员 JWT 访问 `app-server` `/actuator/prometheus` 与 `ai-gateway` `/actuator/prometheus`（非 health 需 ADMIN）。匿名可用 `/actuator/health`。

---

## InstanceDown

**信号：** `up{job=~"app-server|ai-gateway"} == 0` 持续 2 分钟，或 compose healthcheck 失败。

**先看：**

1. `docker compose ps` 与对应服务日志（`app-server` / `ai-gateway`）。
2. 依赖健康：MySQL / Redis / RabbitMQ / Postgres。
3. 最近部署、OOM、磁盘满、`prod` 弱密钥 fail-fast 启动拒绝。

**处理：**

- 依赖未就绪：先修数据面，再重启应用。
- 应用崩溃：按日志 `event=` 与 stack 定位；密钥/token 策略错误需改 env 后重启，不要用占位符绕过。
- 确认 `/actuator/health` 恢复后再关告警。

---

## OutboxBacklog / OutboxFailed / OutboxStale

**信号：** 与 [event-delivery-monitoring.md](event-delivery-monitoring.md) 一致：

- `app.outbox.pending.count` 持续偏高
- `app.outbox.failed.count > 0` 持续 5 分钟
- `app.outbox.oldest.pending.age.seconds > 300` 持续 5 分钟
- 或 RabbitMQ `ai-gateway.knowledge-sync.dlq` 深度 > 0

**先看：**

1. app-server 日志 `event=platform_outbox_publish_failed`、`Rabbit message was returned`。
2. RabbitMQ 管理台：`knowledge-sync` 队列与 DLQ。
3. ai-gateway 消费错误与 Postgres 连通性。

**处理：**

- broker 不可达：恢复 RabbitMQ 后观察 pending 下降。
- 投递被退回：核对 exchange/routing key，勿手工改 outbox 行状态除非已确认消息已消费。
- DLQ 有消息：修消费端后按运维流程重放，避免重复 FULL reindex 打满上游。

---

## AiProviderErrorRate

**信号：** `ai.provider.calls` 中 `outcome != success` 占比 5 分钟内 ≥ 20%（且总量足够，避免冷启动误报）。

常见 `outcome`：`timeout`、`rate_limited`、`circuit_open`、`invalid_response`、`bad_request`、`provider_error`。

**先看：**

1. ai-gateway 日志 `event=ai_provider_call`（`outcome`、`retryable`、`providerStatus`）。
2. 配置中心 active provider / 模型是否被改、密钥是否过期。
3. 上游限流或超时：网关超时已对齐 180s；nginx 反向代理需同步。

**处理：**

- `rate_limited` / `timeout`：降并发、确认日配额与上游配额，勿把 fallback 指到同一上游。
- `circuit_open`：等窗口或修上游后确认 `outcome=success` 恢复。
- 辅导/讲解走规则降级时，前端应显示 `RULE_FALLBACK`，不要当模型结论。

---

## AssessmentHttp5xx

**信号：** `http.server.requests` 中 URI 以 `/api/public/assessments` 开头、`status` 5xx 的 5 分钟速率明显高于基线。

**先看：**

1. app-server 日志对应 `traceId`、问卷 CSRF 头缺失（应为 403 而非 5xx）。
2. MySQL / Redis（公开会话 TTL）、限流（Bucket4j）是否把依赖打挂。
3. 附件上传与对象存储错误是否冒成 500。

**处理：**

- 5xx 伴随 DB 超时：查慢查询与连接池耗尽。
- 仅个别发布编号失败：核对该 `publish` 状态与题库种子，避免对全部参与者回滚。
- 恢复后用一条真实参与码走验证 → 作答保存，确认不再 5xx。

---

## 升级与沟通

- 研究问卷事故优先保护 PII：不要把参与码、姓名、联系方式、IP 打进工单明文。
- schema 变更按 [db-migration-runbook.md](db-migration-runbook.md) 执行 `docs/ddl/`，禁止生产 `docker compose down -v`。
- 研究数据保留与匿名见 [research-data-operations.md](research-data-operations.md)。
