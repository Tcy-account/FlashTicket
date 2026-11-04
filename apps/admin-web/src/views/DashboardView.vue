<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import * as echarts from 'echarts';
import { getOverview, getEventSales, getPurchaseConversion } from '../api';
import type { PlatformOverview, EventSales, PurchaseConversion } from '../api/types';

const overview = ref<PlatformOverview | null>(null);
const eventSales = ref<EventSales[]>([]);
const purchase = ref<PurchaseConversion | null>(null);

const trendRef = ref<HTMLElement | null>(null);
let trendChart: echarts.ECharts | null = null;

function fmtYuan(cent: number): string {
  return (cent / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fmtRate(rate: number): string {
  return (rate * 100).toFixed(1) + '%';
}

function renderTrend() {
  if (!trendRef.value || !overview.value) return;
  if (!trendChart) trendChart = echarts.init(trendRef.value);
  const trend = overview.value.dailyTrend ?? [];
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 24, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: trend.map((t) => t.day), boundaryGap: false },
    yAxis: { type: 'value', name: '实收(元)' },
    series: [
      {
        name: '模拟实收',
        type: 'line',
        smooth: true,
        data: trend.map((t) => t.amountCent / 100),
        areaStyle: { opacity: 0.12 },
        itemStyle: { color: '#635BFF' },
      },
    ],
  });
}

async function load() {
  const [o, e, p] = await Promise.all([
    getOverview(),
    getEventSales(),
    getPurchaseConversion(),
  ]);
  overview.value = o.data ?? null;
  eventSales.value = e.data ?? [];
  purchase.value = p.data ?? null;
  renderTrend();
}

onMounted(load);
onUnmounted(() => trendChart?.dispose());
</script>

<template>
  <div>
    <h2 class="page-title">运营数据看板</h2>
    <p class="page-tip">金额为模拟交易数据，非真实资金收入 · 统计口径见各指标</p>

    <el-row :gutter="16" class="metric-row">
      <el-col :span="4">
        <el-card shadow="never" class="metric">
          <div class="metric-label">模拟实收金额</div>
          <div class="metric-value">¥{{ overview ? fmtYuan(overview.paidAmountCent) : '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="metric">
          <div class="metric-label">已支付订单</div>
          <div class="metric-value">{{ overview?.paidOrderCount ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="metric">
          <div class="metric-label">销售票数</div>
          <div class="metric-value">{{ overview?.soldTicketCount ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="metric">
          <div class="metric-label">成功退款金额</div>
          <div class="metric-value">¥{{ overview ? fmtYuan(overview.refundAmountCent) : '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="metric">
          <div class="metric-label">净收入 / 支付率 / 退款率</div>
          <div class="metric-value">
            ¥{{ overview ? fmtYuan(overview.netRevenueCent) : '-' }}
            <span class="metric-sub">
              {{ overview ? fmtRate(overview.paymentRate) : '-' }} /
              {{ overview ? fmtRate(overview.refundRate) : '-' }}
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>近 7 日模拟实收趋势</template>
          <div ref="trendRef" style="height: 320px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>抢票链路转化</template>
          <div class="conv-list">
            <div class="conv-item">
              <span class="conv-label">受理数</span>
              <span class="conv-value">{{ purchase?.acceptedCount ?? '-' }}</span>
            </div>
            <div class="conv-item">
              <span class="conv-label">成功建单</span>
              <span class="conv-value">{{ purchase?.succeededCount ?? '-' }}</span>
            </div>
            <div class="conv-item">
              <span class="conv-label">失败</span>
              <span class="conv-value">{{ purchase?.failedCount ?? '-' }}</span>
            </div>
            <div class="conv-item">
              <span class="conv-label">处理中</span>
              <span class="conv-value">{{ purchase?.processingCount ?? '-' }}</span>
            </div>
            <el-divider />
            <div class="conv-item">
              <span class="conv-label">抢票成功率</span>
              <span class="conv-value">{{ purchase ? fmtRate(purchase.successRate) : '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>演出销售排行（票面金额，不含优惠券）</template>
      <el-table :data="eventSales" size="small">
        <el-table-column prop="eventTitle" label="活动" min-width="200" />
        <el-table-column prop="orderCount" label="支付订单数" width="120" />
        <el-table-column prop="ticketCount" label="售出票数" width="120" />
        <el-table-column label="票面销售额" width="160">
          <template #default="{ row }">¥{{ fmtYuan(row.amountCent) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
}
.page-tip {
  margin: 0 0 16px;
  font-size: 12px;
  color: #9ca3af;
}
.metric-row {
  margin-bottom: 16px;
}
.metric-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}
.metric-value {
  font-size: 22px;
  font-weight: 600;
  color: #111827;
}
.metric-sub {
  font-size: 13px;
  font-weight: 400;
  color: #6b7280;
}
.conv-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.conv-item {
  display: flex;
  justify-content: space-between;
}
.conv-label {
  color: #6b7280;
}
.conv-value {
  font-weight: 600;
}
</style>
