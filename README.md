# FlashTicket

高并发演出抢票与票务管理平台。Java 21 + Spring Boot 3.5 模块化单体后端，Vue 3 双前端（用户端 ticket-web / 管理端 admin-web），RocketMQ 异步抢票 + Redis 风控准入 + Prometheus/Grafana 监控。

> **当前进度：M00~M17 全部完成**（里程碑 A 业务核心 → B 交易闭环 → C 可靠性 → D 营销与风控 → E 质量与部署收官）。

## 本地环境一键启动

### 1. 启动中间件（Docker Desktop 可折叠分组）

```bash
docker compose up -d
```

> **安全提示**：仓库内不再提供真实默认口令。本地请复制 `.env.example` 为 `.env` 并改掉 `DB_PASSWORD` / `SUPER_ADMIN_PASSWORD` / Grafana 口令后再长期使用；生产见 `docker-compose.prod.yml`（口令必填）。

`docker-compose.yml` 顶层声明了 `name: flashticket`，容器在 Docker Desktop 的 Containers 列表折叠为 **flashticket** 分组，可整组一键启动/停止。

| 服务 | 镜像 | 宿主端口 | 说明 |
|---|---|---|---|
| mysql | mysql:8.4 | 33061 | 库名 flashticket，utf8mb4，UTC，root/${DB_PASSWORD:-changeme}（见 .env.example） |
| redis | redis:7.4 | 16379 | 无密码（本地），会话/限流/库存准入快照 |
| namesrv | apache/rocketmq:5.3.0 | 9876 | RocketMQ 注册中心 |
| broker | apache/rocketmq:5.3.0 | 10911 | 消息存储 |
| proxy | apache/rocketmq:5.3.0 | 8081 (gRPC) / 18080 (HTTP) | 后端 v5 客户端连 gRPC 8081 |
| prometheus | prom/prometheus:v2.55.1 | 9090 | 抓后端 `/actuator/prometheus` |
| grafana | grafana/grafana:11.4.0 | 3000 | admin/${GF_SECURITY_ADMIN_PASSWORD:-changeme}，预置「FlashTicket 运营监控」看板 |

> 端口特意避开本地 3306/6379（本机已装 MySQL 9.3），防止冲突。

**首次启动需预创建 MQ topic**（RocketMQ 不自动建 topic 供消费，缺 topic 会导致消费者启动报 `No topic route info`）：

```bash
docker exec flashticket-broker-1 sh mqadmin updatetopic -n namesrv:9876 -t ft-purchase      -c DefaultCluster
docker exec flashticket-broker-1 sh mqadmin updatetopic -n namesrv:9876 -t ft-order-close  -c DefaultCluster
docker exec flashticket-broker-1 sh mqadmin updatetopic -n namesrv:9876 -t ft-notification -c DefaultCluster
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认激活 `local` profile，连接 localhost:33061 / localhost:16379 / RocketMQ proxy gRPC 8081。

> 若在 WorkBuddy 等会注入 `SERVER__PORT` 环境变量的 IDE 里启动，用 `env -u SERVER__PORT mvn spring-boot:run` 避免 relaxed binding 覆盖 yml 端口。

### 3. 启动前端（pnpm）

```bash
pnpm install   # 首次
pnpm dev       # 一键并行启动双前端
```

| 前端 | 地址 | 技术栈 |
|---|---|---|
| 用户端 ticket-web | http://localhost:5173 | Vue3 + TS + Vite + Naive UI + Tailwind + Pinia |
| 管理端 admin-web | http://localhost:5174 | Vue3 + TS + Vite + Element Plus + SCSS + Pinia |

- 开发专用调试页：http://localhost:5173/dev/api-lab（仅开发环境，生产构建排除）。
- Vite 已配置 `/api`、`/actuator` 代理到 `http://localhost:8080`。

### 4. 验证与监控

| 地址 | 预期 |
|---|---|
| `GET http://localhost:8080/api/v1/health` | 200，`code=SUCCESS`，`data.status=UP` |
| `GET http://localhost:8080/api/v1/health/readiness` | 200，`db=UP` + `redis=UP` |
| `GET http://localhost:8080/actuator/prometheus` | Prometheus 指标（M17 起） |
| http://localhost:9090 | Prometheus（target `flashticket-backend` 应 `up`） |
| http://localhost:3000 | Grafana（默认 admin/changeme，可用环境变量覆盖，预置「FlashTicket 运营监控」看板） |

停掉 flashticket 分组后再次请求 readiness，应返回 **503 + `SERVICE_UNAVAILABLE`**。

## 目录结构

```
FlashTicket/
├── doc/                  # 设计文档 + M00/M17 交付文档（见下方索引）
├── backend/              # Java 后端（单 Maven 工程）
│   ├── src/main/java/com/flashticket/   # 按领域分包
│   │   ├── common/       # 响应信封、错误码、全局异常、请求标识、安全
│   │   ├── system/       # 健康检查
│   │   ├── identity/     # 注册/认证/个人中心/后台 RBAC
│   │   ├── event/        # 分类/场馆/活动/场次/票档/售票计划/审核
│   │   ├── inventory/    # 库存总账/预占/流水/限购/Redis 准入
│   │   ├── purchase/     # 抢票编排（异步 Outbox）
│   │   ├── order/        # 订单/明细/关单
│   │   ├── payment/      # 支付编排/mock 支付
│   │   ├── entitlement/  # 票务权益
│   │   ├── refund/       # 退款
│   │   ├── coupon/       # 优惠券与营销
│   │   ├── risk/         # 限流/风控/黑名单
│   │   ├── notification/ # 站内消息/SSE
│   │   ├── analytics/    # 运营统计
│   │   ├── reconciliation/ # 对账/异常工单/补偿
│   │   └── messaging/    # Outbox/Inbox 事务消息
│   └── src/main/resources/db/migration/   # Flyway V1~V17
├── apps/                 # 前端应用
│   ├── ticket-web/       # 用户端（Naive UI + Tailwind）
│   └── admin-web/        # 管理端（Element Plus + SCSS）
├── packages/             # 共享包（shared-types / api-client / shared-utils）
├── e2e/                  # 独立 E2E + 压测（Playwright 冒烟 + JMeter）
│   ├── global-setup.ts / tests/smoke.spec.ts   # 关键路径冒烟
│   └── loadtest/         # JMeter 抢票压测（checkout.jmx / seed-loadtest.ts）
├── docker/               # nginx / prometheus / grafana / rocketmq 配置
├── docker-compose.yml    # flashticket 中间件分组（含监控栈）
├── package.json          # pnpm dev 一键启动入口
├── pnpm-workspace.yaml   # monorepo 配置
└── README.md
```

## 工程约定

- 数据库：`ft_` 前缀（48 表）；雪花 BIGINT 主键（前端字符串）；金额 BIGINT 分；时间 DATETIME(3) UTC
- API：统一 `/api/v1` 前缀；统一响应信封 `{code, message, data, requestId, traceId, timestamp}`
- 错误码：HTTP 状态码管传输语义，`code` 管业务语义；领域码命名 `{域}_{含义}`
- Flyway：跟随业务开发（V1 空基线，V2~V17 逐里程碑追加）
- 模块边界：业务模块禁止跨域直调 Mapper，统一走「编排服务（purchase/payment/refund）→ 领域服务接口 → 数据访问层」
- 库存防超卖：手写条件更新 `available>=q` 判受影响行数（不用 `@Version` 插件）
- 抢票异步：受理 202 + Outbox 同事务 → RocketMQ → Inbox 幂等建单；三重幂等 + 执行租约 + 延迟关单
- 权限：`@PreAuthorize("hasAuthority({域}:{资源}:{动作})")`，角色 USER/OPERATOR/FINANCE/ADMIN/SUPER_ADMIN

## 文档索引

### 设计文档（doc/）

- `FlashTicket 全项目开发计划.md` — 5 里程碑 18 阶段总纲
- `业务设计.md` / `技术选型.md` — 业务与技术选型基线
- `全局数据库模型与ER关系设计.md` / `全局逻辑ER模型.md` — 字段级数据库设计
- `前端页面设计规范.md` — 前端设计规范
- 各模块设计：`用户与账号模块设计.md`、`演出活动与场次.md`、`库存与票档预占模块.md`、`抢票请求与异步排队模块.md`、`订单与交易状态机模块.md`、`模拟支付与退款模块.md`、`优惠券与营销活动模块.md`、`系统通知与消息中心模块.md`、`风险控制与防刷模块.md`、`运营数据统计与系统对账模块.md`

### M17 交付文档（doc/）

| 文档 | 内容 |
|---|---|
| `M17-05-架构图.md` | 系统分层架构、异步抢票链路、部署拓扑 |
| `M17-05-ER图.md` | 核心交易域 ER 图 + 48 表清单 |
| `M17-05-API清单.md` | 16 域约 90 个端点全量清单 |
| `M17-02-压测报告.md` | JMeter 两库存模式对比（40 QPS / 100% 建单 / 0 错误） |
| `M17-03-故障演练复盘.md` | MQ/Redis 故障注入验证与复盘 |

### 测试与压测

- 后端集成测试：19 个测试类、**183 个用例**（`backend/src/test`，`mvn test`）
- 前端 E2E 冒烟：`cd e2e && npm run test`（注册→登录→抢票→支付→门票→退款关键路径）
- 压测：`cd e2e/loadtest`，JMeter `checkout.jmx`（详见 `doc/M17-02-压测报告.md`）

## 开发计划

见 `doc/FlashTicket 全项目开发计划.md`（5 里程碑 18 阶段）。当前进度：**M17 全部完成**（M00 工程初始化 → M01~M11 业务与交易闭环 → M12~M16 可靠性/营销/风控/对账 → M17 质量·压测·部署收官）。
