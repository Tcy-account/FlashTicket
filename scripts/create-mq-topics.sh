#!/usr/bin/env bash
# 预创建 FlashTicket 本地 RocketMQ topic。
# RocketMQ 5 不会为尚无路由的 topic 自动建消费路由，缺 topic 时消费者启动报
# "No topic route info"。对应 README「本地环境一键启动」。
#
# 用法（仓库根目录，broker 已 healthy）：
#   bash scripts/create-mq-topics.sh
#
# Windows：在 Git Bash 或 WSL 里执行本脚本即可（依赖 Docker Desktop）。
# 若不用 bash，可在 PowerShell 里逐条执行 README 中的三条 docker exec。
#
# 容器名来自 docker-compose.yml 的 name: flashticket + 服务名 broker。
# 若改过 compose project name，设置 BROKER_CONTAINER。

set -euo pipefail

BROKER="${BROKER_CONTAINER:-flashticket-broker-1}"
NAMESRV="${NAMESRV:-namesrv:9876}"
CLUSTER="${CLUSTER:-DefaultCluster}"
TOPICS=(ft-purchase ft-order-close ft-notification)

if ! docker inspect "$BROKER" >/dev/null 2>&1; then
  echo "找不到容器 ${BROKER}。先在仓库根目录执行: docker compose up -d" >&2
  echo "并用 docker ps 确认 broker 容器名，必要时 BROKER_CONTAINER=... bash $0" >&2
  exit 1
fi

for topic in "${TOPICS[@]}"; do
  echo "updatetopic ${topic}"
  docker exec "$BROKER" sh mqadmin updatetopic -n "$NAMESRV" -t "$topic" -c "$CLUSTER"
done

echo "topics ready: ${TOPICS[*]}"
