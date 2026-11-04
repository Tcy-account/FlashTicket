<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { getMyOrders } from '../api';
import type { Order, OrderStatus } from '../api/types';

const router = useRouter();
const message = useMessage();

const orders = ref<Order[]>([]);
const loading = ref(false);
const activeTab = ref<'ALL' | OrderStatus>('ALL');

const tabs = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待支付' },
  { key: 'PAID', label: '已支付' },
  { key: 'CLOSED', label: '已关闭' },
] as const;

const statusMap: Record<OrderStatus, { label: string; cls: string }> = {
  PENDING_PAYMENT: { label: '待支付', cls: 'bg-amber-100 text-amber-700' },
  PAID: { label: '已支付', cls: 'bg-green-100 text-green-700' },
  CLOSED: { label: '已关闭', cls: 'bg-gray-200 text-gray-500' },
};

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

async function load() {
  loading.value = true;
  try {
    const resp = await getMyOrders({
      page: 1,
      pageSize: 100,
      status: activeTab.value === 'ALL' ? undefined : activeTab.value,
    });
    orders.value = resp.data.items;
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '加载订单失败');
  } finally {
    loading.value = false;
  }
}

function openOrder(id: string) {
  router.push(`/orders/${id}`);
}

function switchTab(key: string) {
  activeTab.value = key as 'ALL' | OrderStatus;
  load();
}

function titleOf(o: Order): string {
  return o.items[0]?.eventTitle ?? '演出订单';
}

function tierOf(o: Order): string {
  const item = o.items[0];
  return item ? `${item.tierName} · ${item.quantity} 张` : '';
}

onMounted(load);
</script>

<template>
  <div class="min-h-screen bg-[var(--bg-page)]">
    <header class="sticky top-0 z-10 border-b border-[var(--border)] bg-white">
      <div class="mx-auto flex h-16 max-w-4xl items-center px-6">
        <button class="text-sm text-[var(--text-secondary)] hover:text-[var(--brand)]" @click="router.back()">
          ← 返回
        </button>
        <div class="mx-auto text-xl font-semibold text-[var(--brand)]">我的订单</div>
      </div>
    </header>

    <main class="mx-auto max-w-4xl px-6 py-10">
      <div class="mb-6 flex gap-3">
        <button
          v-for="t in tabs"
          :key="t.key"
          class="rounded-full border px-4 py-1.5 text-sm transition"
          :class="activeTab === t.key
            ? 'border-[var(--brand)] bg-[var(--brand)] text-white'
            : 'border-[var(--border)] bg-white text-[var(--text-secondary)] hover:border-[var(--brand)]'"
          @click="switchTab(t.key)"
        >
          {{ t.label }}
        </button>
      </div>

      <div v-if="loading" class="py-16 text-center text-[var(--text-tertiary)]">加载中…</div>

      <div v-else-if="orders.length" class="space-y-3">
        <div
          v-for="o in orders"
          :key="o.id"
          class="cursor-pointer rounded-xl border border-[var(--border)] bg-white p-5 transition hover:shadow"
          @click="openOrder(o.id)"
        >
          <div class="flex items-center justify-between">
            <div class="font-semibold text-[var(--text-primary)]">{{ titleOf(o) }}</div>
            <span class="rounded-full px-3 py-1 text-xs font-medium" :class="statusMap[o.status].cls">
              {{ statusMap[o.status].label }}
            </span>
          </div>
          <div class="mt-2 flex items-center justify-between text-sm">
            <div class="text-[var(--text-tertiary)]">
              {{ tierOf(o) }}
              <span class="ml-3 text-xs">单号 {{ o.orderNo }}</span>
            </div>
            <div class="font-semibold text-[var(--brand)]">{{ fmtCent(o.payableAmountCent) }}</div>
          </div>
          <div class="mt-1 text-xs text-[var(--text-tertiary)]">下单时间 {{ fmtTime(o.createdAt) }}</div>
        </div>
      </div>

      <div v-else class="rounded-xl border border-dashed border-[var(--border)] bg-white p-12 text-center text-[var(--text-tertiary)]">
        暂无相关订单
      </div>
    </main>
  </div>
</template>
