# 10 分钟演示路径

本文是《系统设计说明书》的附录，正式设计以 `docs/系统设计说明书.md` 为准。

按顺序做。命令在仓库根目录执行。接口与 `PurchaseController`、`PaymentController`、`HealthController`、`doc/M17-05-API清单.md` 一致。字段与 `e2e/global-setup.ts` 相同。

开发页 `http://localhost:5173/dev/api-lab` 目前只把存活/就绪标成可调用（`apps/ticket-web/src/views/ApiLabView.vue` 里登录等项是「待实现」）。抢票链路用下面的 curl，不要指望 API Lab 能走完下单。

本文不写数据库、Grafana 或超管口令。超管账号名与口令读取 `application.yml` 的 `flashticket.bootstrap.super-admin`，运行前把口令放进环境变量 `FT_SUPERADMIN_PASSWORD`（不要把口令写回本文或提交）。连接项见 `docs/中间件使用文档.md`。

## 0–2 分钟：中间件与 Topic

```bash
docker compose up -d
bash scripts/create-mq-topics.sh
```

脚本创建 `ft-purchase`、`ft-order-close`、`ft-notification`。Windows 用 Git Bash 或 WSL；不用 bash 时执行 README 里的三条 `docker exec flashticket-broker-1 sh mqadmin updatetopic …`。

Broker 还没起来时 `mqadmin` 会失败，等 `docker compose ps` 里 broker 健康后再跑一次。

## 2–3 分钟：JWT 与后端

`JwtService` 要 PKCS#8 私钥和 X.509 公钥，路径是 `classpath:jwt/private.pem` 与 `classpath:jwt/public.pem`。`private.pem` 被 gitignore；仓库里的 `public.pem.example` **没有配对私钥**，不能只拷这个公钥。

```bash
mkdir -p backend/src/main/resources/jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  -out backend/src/main/resources/jwt/private.pem
openssl rsa -pubout \
  -in backend/src/main/resources/jwt/private.pem \
  -out backend/src/main/resources/jwt/public.pem
```

```bash
cd backend && mvn spring-boot:run
```

默认 profile `local`，端口 8080。若环境里有 `SERVER__PORT`，用 `env -u SERVER__PORT mvn spring-boot:run`。

另开终端做检查：

```bash
curl -sS http://localhost:8080/api/v1/health
curl -sS http://localhost:8080/api/v1/health/readiness
```

两者都应 `code=SUCCESS`。readiness 的 `data` 里 `db=UP`、`redis=UP`。

可选前端（不挡后面的 curl）：

```bash
pnpm install   # 首次
pnpm dev       # ticket-web :5173 ，admin-web :5174
```

## 3–6 分钟：造一场已开售演出并注册用户

没有 `PUBLISHED` 票档时，抢票会被资格校验拒绝。下面脚本只调用已有管理端接口：超管建档，OPERATOR 提交审核，超管审核并发布（审核人必须不是提交人），再注册一个普通用户。验证码打在后端日志，格式：`[邮箱验证码·本地模拟] 邮箱=… 用途=REGISTER 验证码=……`。Redis 键是 `auth:verify:code:REGISTER:<email>`。

依赖本机 `python3`。把输出里的 `TIER_ID`、`USER_TOKEN`、`ADMIN_TOKEN` 留到下一步。

```bash
python3 << 'PY'
import json, os, urllib.request, time, subprocess

BASE = "http://localhost:8080/api/v1"
# 已有超管口令只从环境变量读，值在 application.yml 的 flashticket.bootstrap.super-admin.password
SA = {"account": "superadmin", "password": os.environ["FT_SUPERADMIN_PASSWORD"]}
# 本脚本当场创建的员工/用户口令，不是仓库里的密钥
DEMO_PASSWORD = os.environ.get("FT_DEMO_PASSWORD", "ChangeMe_Demo_2026")

def call(method, path, body=None, token=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req) as resp:
        payload = json.load(resp)
    if payload.get("code") != "SUCCESS":
        raise SystemExit(payload)
    return payload["data"]

def utc(offset_sec):
    return time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(time.time() + offset_sec))

stamp = format(int(time.time()), "x")
admin = call("POST", "/admin/auth/login", SA)["accessToken"]
roles = call("GET", "/admin/roles", token=admin)
op_role = int(next(r["id"] for r in roles if r["code"] == "OPERATOR"))
op_name = f"demoop_{stamp}"[-32:]
call("POST", "/admin/staff", {
    "username": op_name, "email": f"{op_name}@test.com", "nickname": "演示审核",
    "password": DEMO_PASSWORD, "roleIds": [op_role],
}, admin)
op = call("POST", "/admin/auth/login", {"account": op_name, "password": DEMO_PASSWORD})["accessToken"]
cat = call("POST", "/admin/categories", {"code": f"D{stamp.upper()}"[:32], "name": "演示分类"}, admin)
venue = call("POST", "/admin/venues", {
    "name": "演示场馆", "city": "上海", "address": "演示路 1 号", "maxCapacity": 500,
}, admin)
event = call("POST", "/admin/events", {
    "categoryId": cat["id"], "title": f"演示演出 {stamp}",
    "description": "10 分钟演示", "coverUrl": "https://example.com/cover.jpg",
    "detailContent": "演示详情", "organizerName": "演示主办",
    "attendanceNotice": "请提前入场", "refundPolicy": "演出前可退",
}, admin)
session = call("POST", f"/admin/events/{event['id']}/sessions", {
    "venueId": venue["id"], "name": "演示场次",
    "startAt": utc(7 * 86400), "endAt": utc(7 * 86400 + 7200), "saleCapacity": 100,
}, admin)
tier = call("POST", f"/admin/sessions/{session['id']}/tiers", {
    "name": "演示看台", "unitPriceCent": 10000, "initialQuantity": 50,
    "minPurchase": 1, "maxPurchase": 2, "perUserLimit": 2,
}, admin)
call("PUT", f"/admin/tiers/{tier['id']}/sale-plan", {
    "saleStartAt": utc(-3600), "saleEndAt": utc(30 * 86400),
}, admin)
call("POST", f"/admin/events/{event['id']}/submit-review", token=op)
call("POST", f"/admin/events/{event['id']}/review", {"approve": True, "comment": "演示通过"}, admin)
call("POST", f"/admin/events/{event['id']}/publish", token=admin)

email = f"demouser_{stamp}@test.com"
call("POST", "/auth/verify-code/send", {"email": email, "purpose": "REGISTER"})
code = subprocess.check_output(
    ["docker", "exec", "flashticket-redis-1", "redis-cli", "GET", f"auth:verify:code:REGISTER:{email}"],
    text=True).strip()
username = f"demouser_{stamp}"[:32]
call("POST", "/auth/register", {
    "username": username, "email": email, "verifyCode": code, "password": DEMO_PASSWORD,
})
user = call("POST", "/auth/login", {"account": username, "password": DEMO_PASSWORD})
print("TIER_ID=" + tier["id"])
print("USER_TOKEN=" + user["accessToken"])
print("ADMIN_TOKEN=" + admin)
PY
```

分类码必须是大写字母开头。时间是 UTC 的 `yyyy-MM-ddTHH:mm:ss`，不要带时区后缀。若 Redis 读不到验证码，到后端日志搜「邮箱验证码·本地模拟」。

## 6–8 分钟：202 → 轮询 → 模拟支付 → 库存与门票

```bash
TIER_ID=上一步的票档ID
USER_TOKEN=上一步的用户token
ADMIN_TOKEN=上一步的超管token
REQ_ID=$(python3 -c 'import uuid; print(uuid.uuid4())')

curl -sS -D - -o /tmp/ft-purchase.json \
  -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"requestId\":\"$REQ_ID\",\"tierId\":\"$TIER_ID\",\"quantity\":1}" \
  http://localhost:8080/api/v1/purchases
```

期望：**HTTP 202**，正文 `code=SUCCESS`，`data.status=QUEUED`，`data.order=null`。同一 `requestId` 再 POST 一次应返回已有状态，不会再扣库存。

```bash
curl -sS -H "Authorization: Bearer $USER_TOKEN" \
  "http://localhost:8080/api/v1/purchases/$REQ_ID"
```

等到 `data.status=SUCCEEDED`，记下 `data.order.id`（或 `data.orderId`）。通常一两秒。仍是 `QUEUED` 就看文末故障。

```bash
ORDER_ID=订单ID
PAY=$(curl -sS -X POST -H "Authorization: Bearer $USER_TOKEN" \
  "http://localhost:8080/api/v1/orders/$ORDER_ID/payments")
echo "$PAY"
PAYMENT_ID=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<<"$PAY")
ATTEMPT=$(python3 -c 'import uuid; print(uuid.uuid4())')

curl -sS -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"attemptRequestId\":\"$ATTEMPT\"}" \
  "http://localhost:8080/api/v1/payments/$PAYMENT_ID/mock-pay"

curl -sS -H "Authorization: Bearer $USER_TOKEN" http://localhost:8080/api/v1/entitlements
curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8080/api/v1/admin/inventory/$TIER_ID"
curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8080/api/v1/admin/inventory/$TIER_ID/ledger"
```

支付后订单应为 `PAID`。库存：`sold` 比支付前多 1，`reserved` 回到支付前预占释放后的值（单张演示一般是 reserved 0、sold 1、available = total - 1）。流水里能看到 `RESERVE` 然后 `CONFIRM`。权益列表至少一张票。

同一 `attemptRequestId` 再 mock-pay 一次，不应再确认一遍（幂等）。

公开余量：`GET /api/v1/tiers/{tierId}/availability`（无需登录）。

## 8–10 分钟：健康检查与 Prometheus

```bash
curl -sS http://localhost:8080/actuator/prometheus | head
```

浏览器打开：

- Prometheus `http://localhost:9090` → Status / Targets，`flashticket-backend` 应为 UP（抓取地址 `host.docker.internal:8080`，见 `docker/prometheus/prometheus.yml`）
- Grafana `http://localhost:3000`（账号见 `GF_SECURITY_ADMIN_USER` / `GF_SECURITY_ADMIN_PASSWORD`，勿把口令写入文档）→ 看板「FlashTicket 运营监控」

停掉 compose 分组后再请求 readiness，应是 **HTTP 503**，`code=SERVICE_UNAVAILABLE`（README 已写）。MySQL/Redis 恢复并重启后端后再查应回到 200。

## 失败时先看这里

| 现象 | 原因与处理 |
|---|---|
| 启动日志 `No topic route info`，或请求一直 `QUEUED` | Topic 没建，或建在后端启动之前但消费者已失败退出。先跑 `scripts/create-mq-topics.sh`，再重启 `mvn spring-boot:run`。查 `ft_outbox_event.status`：停在 `PENDING` 说明发布没成功 |
| 后端起不来，`无法加载 JWT 私钥` | `backend/src/main/resources/jwt/private.pem` 与 `public.pem` 成对缺失。按上一节 openssl 生成，不要只拷 `public.pem.example` |
| readiness 503，`code=SERVICE_UNAVAILABLE` | MySQL 或 Redis 不可达。`docker compose ps`，确认 33061 / 16379。正文里 `db` 或 `redis` 会是 `DOWN:…` |
| 抢票 409 / `INVENTORY_SOLD_OUT` 或资格类业务码 | 票档未发布、不在开售窗口，或 Redis 准入快照被手动标成售罄。默认可入场开关是关的，以 MySQL 账本为准 |
| 抢票 429 或限流业务码 | `flashticket.risk.rate-limit.purchase-limit` 默认 5 秒 1 次。换 `requestId` 也要等窗口，或换用户 |
| 支付 `PAYMENT_ORDER_CLOSED` / `PAYMENT_ORDER_EXPIRED` | 订单已不是 `PENDING_PAYMENT`，或 `expire_at` 已过。演示不要把支付期限改成极短后再慢慢操作 |
| 端口被占用 | 后端 8080，MySQL 故意用 33061 避开本机 3306，Redis 用 16379 |
| Grafana 无数据 | 后端必须在宿主机 8080，Prometheus 在容器里通过 `host.docker.internal` 抓。Linux 上该主机名依赖 Docker 版本；抓不到时看 Prometheus target 错误，而不是改业务代码 |

更全的路径与权限点以 `doc/M17-05-API清单.md` 为准。
