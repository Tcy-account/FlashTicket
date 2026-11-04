#!/bin/bash
# M17-02 压测后指标采集：统计真实订单完成吞吐、MQ 积压、库存消耗
# 用法：bash collect.sh <tierId> [时间窗口分钟，默认 10]
set -e
TIER_ID="${1:?用法: bash collect.sh <tierId> [窗口分钟]}"
WINDOW="${2:-10}"

MYSQL="docker exec flashticket-mysql-1 mysql -uroot -p123456 flashticket -N -e"

echo "======== 抢票请求状态分布（近 ${WINDOW} 分钟）========"
$MYSQL "SELECT status, COUNT(*) FROM ft_purchase_request WHERE created_at > NOW() - INTERVAL ${WINDOW} MINUTE GROUP BY status ORDER BY status;"

echo "======== 订单数（近 ${WINDOW} 分钟）========"
$MYSQL "SELECT CONCAT('orders=', COUNT(*)) FROM ft_order WHERE created_at > NOW() - INTERVAL ${WINDOW} MINUTE;"

echo "======== Outbox 积压（当前）========"
$MYSQL "SELECT CONCAT('outbox_', status, '=', COUNT(*)) FROM ft_outbox_event GROUP BY status;"

echo "======== 库存消耗（tier_id=${TIER_ID}）========"
$MYSQL "SELECT CONCAT('total=', total, ' available=', available, ' reserved=', reserved, ' sold=', sold) FROM ft_inventory WHERE tier_id = '${TIER_ID}';"
