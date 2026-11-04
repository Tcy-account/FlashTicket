<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { claimCoupon, getAvailableCoupons } from '../api';
import type { CouponAvailable } from '../api/types';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const message = useMessage();
const auth = useAuthStore();

const coupons = ref<CouponAvailable[]>([]);
const loading = ref(false);
const claimingId = ref<string | null>(null);

const fmtCent = (c: number) => `¥${(c / 100).toFixed(2)}`;

function fmtTime(t: string | null) {
  if (!t) return '-';
  return t.replace('T', ' ').slice(0, 16);
}

/** 优惠规则文案（满减 / 折扣）。 */
function ruleText(c: CouponAvailable): string {
  if (c.type === 'FULL_REDUCTION') {
    const threshold = c.thresholdCent > 0 ? `满 ${fmtCent(c.thresholdCent)} ` : '';
    return `${threshold}减 ${fmtCent(c.discountCent)}`;
  }
  const off = (c.discountRate / 100).toFixed(1).replace(/\.0$/, '');
  const cap = c.maxDiscountCent > 0 ? `，最高优惠 ${fmtCent(c.maxDiscountCent)}` : '';
  return `${off} 折${cap}`;
}

/** 有效期文案。 */
function validityText(c: CouponAvailable): string {
  if (c.validityType === 'RELATIVE_DAYS') {
    return `领取后 ${c.validDays} 天内有效`;
  }
  return `${fmtTime(c.validFrom)} ~ ${fmtTime(c.validUntil)}`;
}

async function load() {
  loading.value = true;
  try {
    const r = await getAvailableCoupons();
    coupons.value = r.data ?? [];
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '加载优惠券失败');
  } finally {
    loading.value = false;
  }
}

async function onClaim(c: CouponAvailable) {
  if (!auth.isLoggedIn) {
    message.warning('请先登录后领券');
    router.push({ path: '/login', query: { redirect: '/coupons' } });
    return;
  }
  claimingId.value = c.templateId;
  try {
    const r = await claimCoupon(genUuid(), c.templateId);
    if (r.data.status === 'SUCCEEDED') {
      message.success('领取成功，已放入我的优惠券');
      load();
    } else {
      message.warning(r.data.failReason ?? '领取失败');
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message ?? '领取失败');
  } finally {
    claimingId.value = null;
  }
}

function genUuid(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
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
        <div class="mx-auto text-xl font-semibold text-[var(--brand)]">领券中心</div>
        <router-link to="/coupons/mine" class="text-sm text-[var(--text-secondary)] hover:text-[var(--brand)]">
          我的券
        </router-link>
      </div>
    </header>

    <main class="mx-auto max-w-4xl px-6 py-10">
      <div v-if="loading" class="py-16 text-center text-[var(--text-tertiary)]">加载中…</div>

      <div v-else-if="coupons.length" class="grid gap-4 md:grid-cols-2">
        <div
          v-for="c in coupons"
          :key="c.templateId"
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
              <div class="font-semibold text-[var(--text-primary)]">{{ c.name }}</div>
              <div class="mt-1 text-xs text-[var(--text-tertiary)]">{{ c.campaignName }}</div>
              <div class="mt-2 text-sm text-[var(--brand)]">{{ ruleText(c) }}</div>
              <div class="mt-2 space-y-0.5 text-xs text-[var(--text-tertiary)]">
                <div>有效期：{{ validityText(c) }}</div>
                <div>余量 {{ c.availableQuantity }} / {{ c.totalQuantity }} · 每人限领 {{ c.perUserLimit }} 张</div>
              </div>
            </div>
          </div>
          <div class="border-t border-[var(--border)] px-4 py-2 text-right">
            <button
              class="rounded-lg bg-[var(--brand)] px-4 py-1.5 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-60"
              :disabled="claimingId === c.templateId"
              @click="onClaim(c)"
            >
              {{ claimingId === c.templateId ? '领取中…' : '立即领取' }}
            </button>
          </div>
        </div>
      </div>

      <div v-else class="rounded-xl border border-dashed border-[var(--border)] bg-white p-12 text-center text-[var(--text-tertiary)]">
        暂无可领取的优惠券
      </div>
    </main>
  </div>
</template>
