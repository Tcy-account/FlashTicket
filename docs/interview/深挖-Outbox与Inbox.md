# 深挖：Outbox 与 Inbox

本文是《系统设计说明书》的附录，正式设计以 `docs/系统设计说明书.md` 为准。机制正文见「4.3.4 Outbox 与 Inbox」。

代码包：`messaging`（`OutboxService`、`OutboxPublisher`、`RocketMQPublisher`、`OutboxEventMapper`、`MessageInboxMapper`）和 `purchase`（`PurchaseCheckoutService`、`PurchaseRequestListener`、`PurchaseRequestProcessor`、`PurchaseOrderBuilder`、`PurchaseRecoveryScanner`）。表在 `V12__outbox_inbox_purchase_lease.sql`：`ft_outbox_event`、`ft_message_inbox`，以及抢票请求上的租约列。

## 机制

RocketMQ 投递是至少一次，而且 HTTP 线程不能等消费完成。做法是把「业务已受理」和「消息已发出」拆成两段，中间用同一数据库把意图留下来。

**受理（Outbox 与业务同行）**  
`PurchaseCheckoutService.accept` 标了 `@Transactional`。新请求会插入：

- `ft_purchase_request`：`QUEUED`，客户端 `requestId`，`accepted_deadline_at` 默认现在起 60 秒
- `ft_outbox_event`：`event_id` 新 UUID，`topic` 默认 `ft-purchase`，`event_type=PURCHASE_SUBMITTED`，`payload` 只有 eventId + requestId，`status=PENDING`

两行同一个本地事务。进程在 202 之后立刻挂掉，请求和待发事件还在 MySQL。抢票受理是手写 insert，不是走 `OutboxService.append`；`OutboxService` 是通知等其它事件后来收敛的同一张表。

**发布**  
`OutboxPublisher`（`@Profile("!test")`）默认每 1 秒扫 `PENDING` 且到了 `next_retry_at` 的行（`batch-size` 100）。每行：

1. `acquireLease`：仍是 `PENDING` 才写入 `lease_owner`、`lease_until`（默认 30 秒），`attempt_count+1`
2. `RocketMQPublisher.sendRaw` 原样转发 payload
3. 成功则 `markSent`（`WHERE lease_owner=自己`）→ `SENT`
4. 失败则 `markRetry`，保持 `PENDING`，`next_retry_at` 默认 +5 秒

租约避免多个实例同时把同一事件发出去。`markSent` 带 `lease_owner`，过期的旧发布者不能把状态盖掉。

**消费（Inbox + 执行租约）**  
`PurchaseRequestListener` 消费组名即 `PurchaseRequestProcessor.CONSUMER_NAME = ft-purchase-cg`。`process` 的顺序：

1. `MessageInboxMapper.countByConsumerAndEvent`。唯一键是 `(consumer_name, event_id)`。已有记录则直接成功结束，不再建单
2. 请求已是 `SUCCEEDED` / `FAILED` / `CANCELLED`：补记 Inbox（重复键忽略）并结束
3. `PurchaseRequestMapper.acquireLease`：仅 `QUEUED` 或 `RECOVERING`，且没有未到期租约，才变成 `PROCESSING` 并写入 `lease_owner`
4. `PurchaseOrderBuilder.build` 同一事务：预占、锁券、建单、`markSucceeded`（必须还是这个 `lease_owner`）、**插入 Inbox**、写通知 Outbox
5. 业务异常（售罄、限购等）：`FAILED` + Inbox，消费结果 `SUCCESS`，MQ 不重试
6. 其它异常：`RECOVERING`，`next_retry_at` 默认 +5 秒，监听器返回 `FAILURE`，交给 MQ 重试

`markSucceeded` 影响行数为 0 会抛 `LeaseLostException`，整笔建单回滚。租约被收走时不会留下半截订单。

Inbox 写在建单事务里，而不是「先 ack 再干活」。事务回滚则 Inbox 也不在，消息可以再来。

**恢复扫描**（`PurchaseRecoveryScanner` → `PurchaseRecoveryService`，默认约 5 秒）：

- `PROCESSING` 且 `lease_until` 已过 → `RECOVERING`，补一条新 Outbox（新 `eventId`，同一个 `requestId`）
- `RECOVERING` 到点 → 先把 `next_retry_at` 往后拨（`claimRecovery`），再补发
- `QUEUED` 超过受理截止 → `FAILED`，原因 `QUEUE_TIMEOUT`

新 eventId 能再次通过 Inbox；旧 eventId 已消费则被 Inbox 挡住。订单侧还有 `request_id` 唯一，所以补发不会造出第二张订单。

## 怎么演示

1. 按 `docs/interview/DEMO-10min.md` 正常下一单：202 且 `QUEUED`，一两秒内轮询变 `SUCCEEDED`。这就是 Publisher 扫到 PENDING 并发出、消费者建单的路径。
2. 停 MQ 再受理（`doc/M17-03-故障演练复盘.md` 做过 broker + proxy）：

```bash
docker stop flashticket-broker-1 flashticket-proxy-1
# 再 POST /api/v1/purchases ，仍应 202 QUEUED
docker start flashticket-broker-1 flashticket-proxy-1
```

停机期间请求保持 `QUEUED`（Outbox 为 `PENDING`）。拉起后等 Publisher 扫描，应变 `SUCCEEDED`。该复盘记录的是 3 次受理、3 条 PENDING、恢复后 3 笔订单。用 MySQL 客户端看 `ft_outbox_event`、`ft_purchase_request` 即可，不要把那次样本说成固定吞吐。

3. 启动期若日志是 `No topic route info`，先 `bash scripts/create-mq-topics.sh` 再重启后端。Topic 缺失时 Publisher 会一直 `markRetry`，请求不会自己变成成功。

重复 POST 同一个 `requestId`：受理层在插库前就返回已有状态，不会再插第二行 Outbox。

## 面试怎么讲

用一句话定义问题：发消息和改数据库不是同一个事务，先发消息可能库没提交，先提交库可能进程在发出去之前死掉。Outbox 把「要发的消息」写成同一事务里的一行，发送变成可重试的后台扫描。

接着主动说至少一次的代价：发布租约减少重复发送，但仍然可能发出两次。所以消费端必须幂等。本项目的幂等不是只靠 MQ 的 messageId，而是三样东西一起：

- Inbox `(consumer_name, event_id)`，和建单提交绑在一起
- 抢票处理租约，同一时刻只有一个 `lease_owner` 能把请求打成 `SUCCEEDED`
- `request_id` 在预占和订单上唯一

恢复补发换 eventId、不换 requestId。旧消息被 Inbox 吃掉，新消息若订单已在，会在终态检查处停住。

不要把 202 的返回速度当成抢到票。`QUEUED` 只说明受理落库。真实结果看轮询状态，压测报告也是把受理 QPS 和订单完成量分开写的（`doc/M17-02-压测报告.md`）。
