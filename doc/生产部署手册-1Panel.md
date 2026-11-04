# FlashTicket 生产部署手册（1Panel · 2C1G · 纯 IP）

> 目标机器：2 核 / 1 GiB 内存 / 30 GiB 系统盘 / 无域名，只有公网 IP。
> 部署形态：单机 Docker Compose 全栈，1Panel 负责 Docker 环境、文件上传、防火墙和可视化查看。

---

## 1. 部署拓扑

```
                      公网 IP
                         │
        ┌────────────────┴─────────────────┐
        │                                  │
   :8080                             :8081
 用户端 ticket-web                管理端 admin-web
        │                                  │
        └──────────┬───────────────────────┘
                   │  gateway (nginx:1.27-alpine)
                   │  /api/*  →  backend:8080
                   │  SSE     →  关闭 proxy_buffering
                   ▼
          ┌─────────────────┐
          │ backend:8080    │  Spring Boot 3.5 / JRE 21（堆 288m）
          │  prod profile   │  Flyway 首次启动自动建表
          └────────┬────────┘
                   │  Docker 内部网络（不映射到宿主机）
      ┌────────────┼──────────────┬─────────────────┐
      ▼            ▼              ▼                 ▼
   mysql:3306   redis:6379   proxy:8081        （可选）监控
   buffer 64M   maxmem 96M    │
                              ├── namesrv:9876
                              └── broker:10911
```

**两个关键设计决定**：

| 决定 | 原因 |
|---|---|
| 管理端独立端口 8081，不用 `/admin/` 子路径 | `admin-web` 的 `createWebHistory()` 没有 base，Vite 默认 `base:'/'`，挂子路径必然白屏 + 资源 404。改 base 要动代码和路由，分端口零成本。 |
| MySQL / Redis / RocketMQ 端口不映射到宿主机 | 映射出去等于把 3306、6379 暴露在公网，弱密码当天就会被爆破。容器间走服务名互通即可。 |

---

## 2. 上线前：服务器准备

### 2.1 加 2G swap（必做，不是可选）

1 GiB 物理内存装不下这套栈，正常 RSS 约 1.4 GB，差额只能靠 swap。`free -h` 先看一眼。

```bash
free -h
fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
sysctl -w vm.swappiness=30
echo 'vm.swappiness=30' >> /etc/sysctl.conf
free -h   # 确认 Swap 一行有 2.0Gi
```

`swappiness=30` 意味着只在内存吃紧时才换出，比默认 60 更保守。

### 2.2 确认 Docker 与端口

1Panel 安装时会带 Docker，先确认：

```bash
docker version          # Client 和 Server 都要有输出
docker compose version  # 需要 v2
ss -lntp | grep -E ':(8080|8081)\b' && echo "端口被占用，改 .env 里的 WEB_PORT/ADMIN_PORT"
```

### 2.3 配镜像加速（国内服务器必做）

1Panel 左侧 **容器 → 配置 → 镜像加速**，填入你的加速地址（阿里云控制台「容器镜像服务 → 镜像加速器」可拿到专属地址），例如：

```
https://<你的ID>.mirror.aliyuncs.com
```

保存后 `docker info | grep -A3 "Registry Mirrors"` 验证。不配的话拉 `mysql:8.4` + `apache/rocketmq:5.3.0` + `eclipse-temurin:21-jre-jammy` 会非常慢，甚至超时失败。

### 2.4 放行端口（两处都要放，漏一处就访问不了）

1. **云厂商控制台安全组**：入方向放行 TCP 8080、8081（1Panel 面板端口别关）。
2. **1Panel 防火墙**：左侧 **主机 → 防火墙**（部分版本在「安全」下），添加 8080、8081 的 TCP 放行规则。

---

## 3. 本地构建产物（服务器上不编译）

> **为什么不在服务器编译**：`mvn package` 一个 Spring Boot 项目峰值内存 600 MB 以上，1 GiB 机器 + 已经跑起来的 MySQL/Redis 会直接 OOM Killer。前端 `vite build` 同样吃内存。**本地构建、只传结果**是低配服务器的唯一正确做法。

### 3.1 后端打 jar

```bash
cd F:/tcy_project/FlashTicket/backend
mvn clean package -DskipTests
ls -lh target/flashticket-backend-0.0.1-SNAPSHOT.jar   # 约 50-70 MB
```

### 3.2 前端打静态包

```bash
cd F:/tcy_project/FlashTicket
pnpm build
ls apps/ticket-web/dist apps/admin-web/dist
```

Windows 下如果 `pnpm build` 报 `SAFE_DELETE_BULK_CONFIRM_REQUIRED`（清理 dist 被拦），先手动删目录：

```powershell
Remove-Item -Recurse -Force apps/ticket-web/dist, apps/admin-web/dist
```

### 3.3 整理成一个部署包

把散落的产物聚成服务器需要的目录结构：

```bash
cd F:/tcy_project/FlashTicket
rm -rf .deploy && mkdir -p .deploy/backend/target .deploy/html .deploy/docker/nginx .deploy/docker/rocketmq

cp docker-compose.prod.yml .env.example .deploy/
cp docker/nginx/nginx.prod.conf .deploy/docker/nginx/
cp docker/rocketmq/broker.prod.conf .deploy/docker/rocketmq/
cp backend/target/flashticket-backend-*.jar .deploy/backend/target/
cp -r apps/ticket-web/dist .deploy/html/ticket-web
cp -r apps/admin-web/dist .deploy/html/admin-web

tar -czf flashticket-deploy.tar.gz -C .deploy .
ls -lh flashticket-deploy.tar.gz
```

**注意 `.env` 没有打进包里**。数据库密码、超管密码不该走公网传输，到服务器上从 `.env.example` 复制再改。

---

## 4. 上传并启动

### 4.1 上传

1Panel 左侧 **文件** → 进入 `/opt` → 新建目录 `flashticket` → 上传 `flashticket-deploy.tar.gz`。
或者用命令行（本地执行）：

```bash
scp flashticket-deploy.tar.gz root@<服务器IP>:/opt/flashticket/
```

### 4.2 解压 + 填密码

1Panel 左侧 **终端**（或 SSH）：

```bash
mkdir -p /opt/flashticket && cd /opt/flashticket
tar -xzf flashticket-deploy.tar.gz
cp .env.example .env
vim .env        # 改 DB_PASSWORD / SUPER_ADMIN_PASSWORD，至少 12 位
```

`.env` 里两个必填项，compose 用了 `:?` 语法强制校验，为空会直接报错拒绝启动：

```ini
DB_PASSWORD=<改成强密码>
SUPER_ADMIN_PASSWORD=<12-64 位>
```

> **密码改了但起不来？** MySQL 的 root 密码只在**数据卷为空时**初始化。如果之前已经启动过、改了 `DB_PASSWORD`，MySQL 仍用旧密码，后端会报 `Access denied`。解决：
> `docker compose -f docker-compose.prod.yml down -v`（**会删库**）后重来，或进容器用旧密码 `ALTER USER` 改。

### 4.3 构建后端镜像并启动

```bash
cd /opt/flashticket
docker compose -f docker-compose.prod.yml up -d --build
```

首次会依次发生：拉镜像（慢，视网速 5-15 分钟）→ MySQL 初始化并建库（约 40 秒）→ mq-init 等 broker 注册并预建 topic → 后端启动跑 Flyway（首次约 60-90 秒）→ 网关就绪。

**别急着访问，先看状态**：

```bash
docker compose -f docker-compose.prod.yml ps
```

期望结果：`mysql`、`redis`、`namesrv`、`broker`、`proxy`、`backend`、`gateway` 全部 `Up`，`mq-init` 是 `Exited (0)`（一次性任务正常退出）。

---

## 5. 验收

```bash
# 1) 后端健康检查
curl -s http://127.0.0.1:8080/actuator/health
# 期望 {"status":"UP"}

# 2) 公开接口能返回数据（Flyway 已建表）
curl -s "http://127.0.0.1:8080/api/v1/events?page=1&pageSize=5" | head -c 300

# 3) 前端静态页
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/

# 4) 内存实况
docker stats --no-stream
free -h
```

浏览器：

- 用户端 `http://<公网IP>:8080`
- 管理端 `http://<公网IP>:8081`，用 `.env` 里的 `SUPER_ADMIN_*` 登录

管理端首次进入需要数据：先建**分类 → 场馆 → 活动 → 场次 → 票档 → 售票计划**，提交审核并通过后发布，用户端才能看到并抢票。

---

## 6. 日常运维

### 6.1 查看日志

```bash
cd /opt/flashticket
docker compose -f docker-compose.prod.yml logs -f backend --tail 200
docker compose -f docker-compose.prod.yml logs -f broker
```

日志已配 `max-size=10m / max-file=3`，单服务最多占 30 MB 磁盘，不会把 30G 盘写满。

### 6.2 只更新后端

```bash
# 本地重新打包后上传 jar，覆盖到 /opt/flashticket/backend/target/
cd /opt/flashticket
docker compose -f docker-compose.prod.yml up -d --build backend
```

`restart: always` + `shutdown: graceful` 会让在途请求处理完再切换。

### 6.3 只更新前端

前端是 nginx 直接读挂载目录，**不用重启容器**：

```bash
# 本地重新 pnpm build 后上传覆盖
cd /opt/flashticket
tar -xzf ticket-web-dist.tar.gz -C html/ticket-web
```

发版后 `index.html` 已设 `Cache-Control: no-cache`，客户端不会拿到旧壳。

### 6.4 备份数据库

```bash
cd /opt/flashticket
docker compose -f docker-compose.prod.yml exec -T mysql \
  mysqldump -uroot -p"$(grep DB_PASSWORD .env | cut -d= -f2)" --single-transaction flashticket \
  > /opt/backup/flashticket-$(date +%F).sql
```

放进 1Panel 的 **计划任务**（Shell 脚本，每天凌晨 3 点），保留最近 7 份。

---

## 7. 故障排查对照表

| 现象 | 原因 | 处理 |
|---|---|---|
| 浏览器打不开，`curl 127.0.0.1:8080` 正常 | 云安全组或 1Panel 防火墙没放行 | 两处都放行 8080/8081 |
| 后端容器反复重启，日志 `Communications link failure` | MySQL 未就绪或密码不匹配 | 看 `logs mysql`；确认 `.env` 的密码与首次初始化时一致 |
| 后端启动报 `No topic route info` | topic 未预建 | 看 `logs mq-init`；手动执行 `docker compose ... exec broker sh mqadmin updatetopic -n namesrv:9876 -t ft-purchase -c DefaultCluster`（三个 topic 都要建） |
| `docker ps` 显示容器 `Restarting`，`docker inspect` 里 `OOMKilled: true` | 该服务超了 `mem_limit` | `docker stats` 找出吃内存的；先降后端 `-Xmx` 或停掉非核心服务 |
| 管理端白屏，控制台 404 `/assets/*` | 访问了错误端口 | 管理端是 `:8081`，不是 `:8080/admin` |
| 页面能开，接口全 502 | 后端挂了或网关未连上 | `logs backend`、`logs gateway` |
| 通知不实时弹出，刷新才有 | SSE 被 nginx 缓冲 | 确认 `location = /api/v1/notifications/stream` 段存在且 `proxy_buffering off` |
| 抢票报限流，明明只点了一次 | `X-Forwarded-For` 未透传，所有用户共用一个 IP 桶 | 检查 nginx `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for` |
| 时间差 8 小时 | 容器时区不是 UTC | 三处必须一致：MySQL `--default-time-zone=+00:00`、后端 `-Duser.timezone=UTC`、容器 `TZ=UTC` |

---

## 8. 为什么不用 1Panel 应用商店装 MySQL/Redis

能装，但这套项目不适合：

1. **参数不可控**：商店的 MySQL 默认 `performance_schema` 开启，1 GiB 机器上光这一项就多吃 100-200 MB。本手册的 compose 里那串 `--innodb-*` 参数是这台机器的生存前提。
2. **端口默认映射到宿主机**：等于把数据库暴露在公网。本手册里中间件只在 Docker 网络内。
3. **生命周期割裂**：应用商店装的服务和你的 compose 不同项目，`down` 时容易漏掉，重建环境无法一键复现。

1Panel 在这套方案里的角色是：**Docker 运行时 + 文件上传 + 终端 + 防火墙 + 容器状态可视化**，这已经够用。

---

## 9. 后续扩展路径

| 需求 | 动作 |
|---|---|
| 有域名了，要 HTTPS | 1Panel **网站 → 创建网站**，指向 `127.0.0.1:8080`，申请 Let's Encrypt 证书；管理端单独建站指向 `8081`，或改用子域名 `admin.xxx.com` |
| 内存升到 4G | `mem_limit` 全面放宽，后端 `-Xmx1g`，加回 Prometheus + Grafana（compose 里加 `profiles: [monitor]`） |
| 要跑压测 | 必须升配。1C1G 下 JMeter 40 QPS 的结论不成立，swap 会掩盖真实延迟 |
| 上多实例 | 后端无状态（会话在 Redis），可直接 `--scale backend=2`；但 RocketMQ 消费者要确认消费组负载均衡生效，MySQL 连接池总数别超过 50 |
