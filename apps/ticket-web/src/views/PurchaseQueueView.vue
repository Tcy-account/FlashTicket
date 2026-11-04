<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { cancelPurchase, getPurchaseStatus } from '../api';
import type { PurchaseStatusResponse } from '../api/types';

const route = useRoute();
const router = useRouter();
const message = useMessage();

const requestId = route.params.requestId as string;

const status = ref<PurchaseStatusResponse | null>(null);
const loading = ref(true);
const cancelLoading = ref(false);
const pollError = ref(false);

let timer: ReturnType<typeof setInterval> | null = null;

/** 终态：不再轮询。 */
const isFinal = (s?: string) =>
  s === 'SUCCEEDED' || s === 'FAILED' || s === 'CANCELLED';

/** 排队中（含处理中/恢复中，统一按「等待/处理」呈现，不虚构排名）。 */
const isPending = (s?: string) =>
  s === 'QUEUED' || s === 'PROCESSING' || s === 'RECOVERING';

/** 失败原因文案映射（后端 ErrorCode 名称，未知则通用兜底）。 */
const failReasonText: Record<string, string> = {
  INVENTORY_SOLD_OUT: '很遗憾，可售库存不足，本次未抢到',
  INVENTORY_LIMIT_EXCEEDED: '已超出该票档每人限购数量',
  INVENTORY_QUANTITY_INVALID: '购买数量非法',
  PURCHASE_EVENT_NOT_PUBLISHED: '该活动当前不可购票',
  PURCHASE_SESSION_NOT_SALE: '该场次当前不可售票',
  PURCHASE_SALE_PAUSED: '该票档售票已暂停',
  PURCHASE_WINDOW_NOT_OPEN: '不在售票时间窗口内',
  QUEUE_TIMEOUT: '排队等待超时，本次未抢到',
  TECHNICAL: '系统繁忙，请稍后重试',
};

function failText(reason: string | null): string {
  if (!reason) return '本次未抢到，请稍后重试';
  return failReasonText[reason] ?? '本次未抢到，请稍后重试';
}

async function refresh() {
  if (status.value && isFinal(status.value.status)) {
    stopPolling();
    return;
  }
  try {
    const resp = await getPurchaseStatus(requestId);
    status.value = resp.data;
    loading.value = false;
    pollError.value = false;
    if (isFinal(resp.data.status)) {
      stopPolling();
    }
  } catch (e: any) {
    loading.value = false;
    pollError.value = true;
    message.error(e?.response?.data?.message ?? '查询抢票状态失败');
  }
}

function startPolling() {
  refresh();
  timer = setInterval(refresh, 2000);
}

function stopPolling() {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}

async function doCancel() {
  cancelLoading.value = true;
  try {
    const resp = await cancelPurchase(requestId);
    status.value = resp.data;
    stopPolling();
    message.success('已取消排队');
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '取消失败');
  } finally {
    cancelLoading.value = false;
  }
}

function goOrder() {
  const orderId = status.value?.order?.id ?? status.value?.orderId;
  if (orderId) router.push(`/orders/${orderId}`);
}

const fmtCent = (c?: number) => (c == null ? '' : `¥${(c / 100).toFixed(2)}`);

onMounted(startPolling);
onUnmounted(stopPolling);
</script>

<template>
  <div class="min-h-screen bg-[var(--bg-page)]">
    <header class="sticky top-0 z-10 border-b border-[var(--border)] bg-white">
      <div class="mx-auto flex h-16 max-w-2xl items-center px-6">
        <button class="text-sm text-[var(--text-secondary)] hover:text-[var(--brand)]" @click="router.push('/')">
          ← 首页
        </button>
        <div class="mx-auto text-lg font-semibold text-[var(--brand)]">抢票进度</div>
      </div>
    </header>

    <main class="mx-auto max-w-2xl px-6 py-10">
      <!-- 加载中 -->
      <div v-if="loading" class="rounded-2xl border border-[var(--border)] bg-white p-10 text-center text-[var(--text-tertiary)]">
        正在查询抢票状态…
      </div>

      <!-- 轮询失败 -->
      <div v-else-if="pollError" class="rounded-2xl border border-[var(--border)] bg-white p-10 text-center">
        <div class="text-[var(--text-secondary)]">暂时无法获取抢票状态</div>
        <button class="mt-4 rounded-lg bg-[var(--brand)] px-4 py-2 text-sm font-medium text-white" @click="refresh">
          重试
        </button>
      </div>

      <!-- 排队中 / 处理中 -->
      <div v-else-if="isPending(status?.status)" class="rounded-2xl border border-[var(--border)] bg-white p-10 text-center">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[var(--brand)]/10">
          <n-spin size="large" />
        </div>
        <h2 class="mt-6 text-xl font-semibold text-[var(--text-primary)]">
          {{ status?.status === 'QUEUED' ? '排队中' : '正在处理' }}
        </h2>
        <p class="mt-2 text-sm text-[var(--text-secondary)]">
          抢票请求已受理，正在等待处理，请勿刷新或关闭页面
        </p>
        <!-- 明确告知：排队中不代表抢票成功，不展示排名/等待时间 -->
        <p class="mt-4 rounded-lg bg-[var(--bg-page)] px-4 py-3 text-xs text-[var(--text-tertiary)]">
          排队中不代表已抢到票，系统正在按顺序处理请求，请耐心等待最终结果
        </p>
        <div v-if="status?.status === 'QUEUED'" class="mt-6">
          <button
            class="rounded-lg border border-[var(--border)] px-4 py-2 text-sm text-[var(--text-secondary)] hover:text-red-500 disabled:opacity-50"
            :disabled="cancelLoading"
            @click="doCancel"
          >
            {{ cancelLoading ? '取消中…' : '取消排队' }}
          </button>
        </div>
      </div>

      <!-- 成功 -->
      <div v-else-if="status?.status === 'SUCCEEDED'" class="rounded-2xl border border-green-200 bg-white p-10 text-center">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-50 text-3xl text-green-500">
          ✓
        </div>
        <h2 class="mt-6 text-xl font-semibold text-[var(--text-primary)]">抢票成功，订单已创建</h2>
        <div v-if="status.order" class="mt-4 space-y-1 text-sm text-[var(--text-secondary)]">
          <div>订单号：{{ status.order.orderNo }}</div>
          <div>应付金额：{{ fmtCent(status.order.payableAmountCent) }}</div>
        </div>
        <button class="mt-6 rounded-lg bg-[var(--brand)] px-6 py-2 text-sm font-medium text-white" @click="goOrder">
          去订单详情支付
        </button>
      </div>

      <!-- 失败 -->
      <div v-else-if="status?.status === 'FAILED'" class="rounded-2xl border border-[var(--border)] bg-white p-10 text-center">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 text-3xl text-[var(--text-tertiary)]">
          ✕
        </div>
        <h2 class="mt-6 text-xl font-semibold text-[var(--text-primary)]">未抢到</h2>
        <p class="mt-2 text-sm text-[var(--text-secondary)]">{{ failText(status.failReason) }}</p>
        <button class="mt-6 rounded-lg border border-[var(--border)] px-4 py-2 text-sm text-[var(--text-secondary)]" @click="router.back()">
          返回
        </button>
      </div>

      <!-- 已取消 -->
      <div v-else-if="status?.status === 'CANCELLED'" class="rounded-2xl border border-[var(--border)] bg-white p-10 text-center">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 text-3xl text-[var(--text-tertiary)]">
          —
        </div>
        <h2 class="mt-6 text-xl font-semibold text-[var(--text-primary)]">已取消排队</h2>
        <p class="mt-2 text-sm text-[var(--text-secondary)]">本次抢票请求已撤销，未创建订单</p>
        <button class="mt-6 rounded-lg border border-[var(--border)] px-4 py-2 text-sm text-[var(--text-secondary)]" @click="router.back()">
          返回
        </button>
      </div>
    </main>
  </div>
</template>
