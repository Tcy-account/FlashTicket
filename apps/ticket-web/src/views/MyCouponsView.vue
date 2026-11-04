<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { getMyCoupons } from '../api';
import type { UserCoupon, UserCouponStatus } from '../api/types';

const router = useRouter();
const message = useMessage();

const coupons = ref<UserCoupon[]>([]);
const loading = ref(false);
const activeTab = ref<'ALL' | UserCouponStatus>('ALL');

const tabs = [
  { key: 'ALL', label: '全部' },
  { key: 'AVAILABLE', label: '可用' },
  { key: 'LOCKED', label: '已锁定' },
  { key: 'USED', label: '已使用' },
  { key: 'EXPIRED', label: '已过期' },
] as const;

const statusMap: Record<UserCouponStatus, { label: string; cls: string }> = {
  AVAILABLE: { label: '可用', cls: 'bg-green-100 text-green-700' },
  LOCKED: { label: '已锁定', cls: 'bg-amber-100 text-amber-700' },
  USED: { label: '已使用', cls: 'bg-gray-200 text-gray-500' },
  EXPIRED: { label: '已过期', cls: 'bg-gray-200 text-gray-500' },
  VOID: { label: '已作废', cls: 'bg-gray-200 text-gray-500' },
};

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

function ruleText(c: UserCoupon): string {
  if (c.type === 'FULL_REDUCTION') {
    const threshold = c.thresholdCent > 0 ? `满 ${fmtCent(c.thresholdCent)} ` : '';
    return `${threshold}减 ${fmtCent(c.discountCent)}`;
  }
  const off = (c.discountRate / 100).toFixed(1).replace(/\.0$/, '');
  const cap = c.maxDiscountCent > 0 ? `，最高优惠 ${fmtCent(c.maxDiscountCent)}` : '';
  return `${off} 折${cap}`;
}

async function load() {
  loading.value = true;
  try {
    const r = await getMyCoupons(activeTab.value === 'ALL' ? undefined : activeTab.value);
    coupons.value = r.data ?? [];
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '加载优惠券失败');
  } finally {
    loading.value = false;
  }
}

function switchTab(key: string) {
  activeTab.value = key as 'ALL' | UserCouponStatus;
  load();
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
        <div class="mx-auto text-xl font-semibold text-[var(--brand)]">我的优惠券</div>
        <router-link to="/coupons" class="text-sm text-[var(--text-secondary)] hover:text-[var(--brand)]">
          领券中心
        </router-link>
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

      <div v-else-if="coupons.length" class="grid gap-4 md:grid-cols-2">
        <div
          v-for="c in coupons"
          :key="c.id"
          class="overflow-hidden rounded-xl border border-[var(--border)] bg-white"
        >
          <div class="flex items-stretch">
            <div class="flex w-28 shrink-0 flex-col items-center justify-center bg-gradient-to-br from-[var(--brand)] to-[#7c3aed] p-4 text-white">
              <div v-if="c.type === 'FULL_REDUCTION'" class="text-2xl font-bold">
                {{ (c.discountCent / 100).toFixed(0) }}
              </div>
              <div v-else class="text-2xl font-bold">{{ (c.discountRate / 100).toFixed(1).replace(/\.0$/, '') }}</div>
              <div class="mt-1 text-xs">{{ c.type === 'FULL_REDUCTION' ? '元' : '折' }}</div>
            </div>
            <div class="flex-1 p-4">
              <div class="flex items-center justify-between">
                <div class="font-semibold text-[var(--text-primary)]">{{ c.templateName }}</div>
                <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusMap[c.status].cls">
                  {{ statusMap[c.status].label }}
                </span>
              </div>
              <div class="mt-2 text-sm text-[var(--brand)]">{{ ruleText(c) }}</div>
              <div class="mt-2 space-y-0.5 text-xs text-[var(--text-tertiary)]">
                <div>有效期：{{ fmtTime(c.validFrom) }} ~ {{ fmtTime(c.validUntil) }}</div>
                <div>券号 {{ c.couponNo }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="rounded-xl border border-dashed border-[var(--border)] bg-white p-12 text-center text-[var(--text-tertiary)]">
        暂无相关优惠券
      </div>
    </main>
  </div>
</template>
